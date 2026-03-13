package com.nexusvoice.application.rag.service;

import com.nexusvoice.application.rag.dto.KnowledgeBaseCreateRequest;
import com.nexusvoice.application.rag.dto.KnowledgeBaseFileResponse;
import com.nexusvoice.application.rag.dto.KnowledgeBaseResponse;
import com.nexusvoice.application.rag.dto.RagCitationContextDto;
import com.nexusvoice.application.rag.dto.RagSearchResultDto;
import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.entity.KnowledgeBase;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;
import com.nexusvoice.domain.rag.repository.DocumentUnitRepository;
import com.nexusvoice.domain.rag.repository.FileDetailRepository;
import com.nexusvoice.domain.rag.repository.KnowledgeBaseRepository;
import com.nexusvoice.domain.rag.repository.VectorStoreRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.rag.service.DocumentRetrievalService;
import com.nexusvoice.infrastructure.rag.service.DocumentVectorizationServiceImpl;
import com.nexusvoice.infrastructure.rag.service.RagQueryPlanner;
import com.nexusvoice.utils.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagApplicationService {

    private static final int MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final int CHUNK_MAX_CHARS = 700;
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(\\d+)(?:\\s*-\\s*(\\d+))?");

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileDetailRepository fileDetailRepository;
    private final DocumentUnitRepository documentUnitRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final DocumentVectorizationServiceImpl documentVectorizationService;
    private final DocumentRetrievalService documentRetrievalService;
    private final RagQueryPlanner ragQueryPlanner;

    public List<KnowledgeBaseResponse> listKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserId(userId).stream()
                .map(this::toKnowledgeBaseResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseCreateRequest request, Long userId) {
        knowledgeBaseRepository.findByUserIdAndName(userId, request.getName().trim()).ifPresent(existing -> {
            throw new BizException(ErrorCodeEnum.DATA_ALREADY_EXISTS, "知识库名称已存在");
        });

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.onCreate();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(request.getName().trim());
        knowledgeBase.setDescription(trimToNull(request.getDescription()));
        knowledgeBase.setStatus(KnowledgeBaseStatus.ACTIVE);
        knowledgeBase.setFileCount(0);
        knowledgeBase.setTotalSize(0L);
        knowledgeBase.setDocumentCount(0);

        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        return toKnowledgeBaseResponse(saved);
    }

    public KnowledgeBaseResponse getKnowledgeBase(Long knowledgeBaseId, Long userId) {
        return toKnowledgeBaseResponse(getOwnedKnowledgeBase(knowledgeBaseId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseFileResponse uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId) {
        KnowledgeBase knowledgeBase = getOwnedKnowledgeBase(knowledgeBaseId, userId);
        validateUpload(file);

        FileType fileType = resolveFileType(file.getOriginalFilename());
        byte[] bytes = readBytes(file);
        String extractedText = extractText(fileType, bytes);
        if (!StringUtils.hasText(extractedText)) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件中没有可用于检索的文本内容");
        }

        List<String> chunks = splitIntoChunks(extractedText);
        if (chunks.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文档切分后内容为空");
        }

        knowledgeBase.setProcessing();
        knowledgeBase.onUpdate();
        knowledgeBaseRepository.update(knowledgeBase);

        FileDetail fileDetail = new FileDetail();
        fileDetail.onCreate();
        fileDetail.setUserId(userId);
        fileDetail.setKnowledgeBaseId(knowledgeBaseId);
        fileDetail.setFileName(generateStorageName(file.getOriginalFilename()));
        fileDetail.setOriginalName(file.getOriginalFilename());
        fileDetail.setFileType(fileType);
        fileDetail.setMimeType(file.getContentType());
        fileDetail.setFileSize(file.getSize());
        fileDetail.setStorageProvider("INLINE");
        fileDetail.setStorageKey("inline://" + knowledgeBaseId + "/" + fileDetail.getFileName());
        fileDetail.setFilePath(fileDetail.getStorageKey());
        fileDetail.setFileHash(DigestUtils.md5DigestAsHex(bytes));
        fileDetail.setFilePageSize(chunks.size());
        fileDetail.startProcessing();
        fileDetail.startParsing();
        fileDetailRepository.save(fileDetail);

        try {
            fileDetail.startSplitting();
            fileDetailRepository.update(fileDetail);

            List<DocumentUnit> units = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                DocumentUnit unit = new DocumentUnit();
                unit.onCreate();
                unit.setFileId(fileDetail.getId());
                unit.setKnowledgeBaseId(knowledgeBaseId);
                unit.setContent(chunks.get(i));
                unit.setPage(i + 1);
                unit.setIsOcr(fileType == FileType.PDF);
                unit.setIsVector(false);
                units.add(unit);
            }
            documentUnitRepository.saveAll(units);
            log.info("RAG文档切片完成，knowledgeBaseId={}, fileId={}, chunkCount={}",
                    knowledgeBaseId, fileDetail.getId(), units.size());

            int vectorizedCount = 0;
            try {
                fileDetail.setProcessStatus(ProcessStatus.VECTORIZING);
                fileDetailRepository.update(fileDetail);
                vectorizedCount = documentVectorizationService.vectorizeFileDocuments(fileDetail.getId());
                log.info("RAG向量化完成，knowledgeBaseId={}, fileId={}, vectorizedCount={}",
                        knowledgeBaseId, fileDetail.getId(), vectorizedCount);
            } catch (Exception vectorizeEx) {
                log.warn("RAG向量化失败，降级为关键词检索，knowledgeBaseId={}, fileId={}, error={}",
                        knowledgeBaseId, fileDetail.getId(), vectorizeEx.getMessage());
            }

            fileDetail.setProcessStatus(ProcessStatus.COMPLETED);
            fileDetail.setProcessProgress(BigDecimal.valueOf(100));
            fileDetail.setProcessedAt(LocalDateTime.now());
            fileDetail.setCurrentProcessPage(chunks.size());
            fileDetailRepository.update(fileDetail);

            knowledgeBase.addFile(fileDetail.getFileSize());
            knowledgeBase.setDocumentCount(knowledgeBase.getDocumentCount() + chunks.size());
            knowledgeBase.setStatus(KnowledgeBaseStatus.ACTIVE);
            knowledgeBase.onUpdate();
            knowledgeBaseRepository.update(knowledgeBase);
            log.info("RAG文档入库完成，knowledgeBaseId={}, fileId={}, chunkCount={}, vectorizedCount={}",
                    knowledgeBaseId, fileDetail.getId(), chunks.size(), vectorizedCount);

            return toKnowledgeBaseFileResponse(fileDetail);
        } catch (Exception ex) {
            log.error("RAG文档入库失败，knowledgeBaseId={}, fileId={}, error={}",
                    knowledgeBaseId, fileDetail.getId(), ex.getMessage(), ex);
            fileDetail.markFailed("RAG_INGEST_FAILED", ex.getMessage());
            fileDetailRepository.update(fileDetail);
            knowledgeBase.setStatus(KnowledgeBaseStatus.ACTIVE);
            knowledgeBase.onUpdate();
            knowledgeBaseRepository.update(knowledgeBase);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "RAG文档入库失败: " + ex.getMessage(), ex);
        }
    }

    public List<RagSearchResultDto> search(Long knowledgeBaseId, String query, int limit, Long userId) {
        getOwnedKnowledgeBase(knowledgeBaseId, userId);
        RagQueryPlanner.RagQueryPlan queryPlan = ragQueryPlanner.plan(query);
        return documentRetrievalService.hybridSearch(
                        queryPlan.normalizedQuery(),
                        queryPlan.retrievalQueries(),
                        knowledgeBaseId,
                        Math.max(1, limit)
                ).stream()
                .map(this::toSearchResult)
                .toList();
    }

    public RagCitationContextDto getCitationContext(Long knowledgeBaseId,
                                                    Long fileId,
                                                    String location,
                                                    Integer window,
                                                    Long userId) {
        KnowledgeBase knowledgeBase = getOwnedKnowledgeBase(knowledgeBaseId, userId);
        FileDetail fileDetail = fileDetailRepository.findById(fileId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "文件不存在"));
        if (!knowledgeBaseId.equals(fileDetail.getKnowledgeBaseId()) || !userId.equals(fileDetail.getUserId())) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "无权访问该文件");
        }

        Range range = parseRange(location);
        int contextWindow = Math.max(0, Math.min(window != null ? window : 1, 5));
        int fromPage = Math.max(1, range.start() - contextWindow);
        int toPage = range.end() + contextWindow;

        List<RagCitationContextDto.ContextSegmentDto> segments = documentUnitRepository.findByFileId(fileId).stream()
                .filter(unit -> unit.getPage() != null)
                .filter(unit -> unit.getPage() >= fromPage && unit.getPage() <= toPage)
                .sorted(Comparator.comparing(DocumentUnit::getPage))
                .map(unit -> RagCitationContextDto.ContextSegmentDto.builder()
                        .page(unit.getPage())
                        .hit(unit.getPage() >= range.start() && unit.getPage() <= range.end())
                        .content(unit.getContent())
                        .build())
                .toList();

        if (segments.isEmpty()) {
            throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "未找到对应上下文片段");
        }

        return RagCitationContextDto.builder()
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .fileId(fileDetail.getId())
                .fileName(fileDetail.getOriginalName() != null ? fileDetail.getOriginalName() : fileDetail.getFileName())
                .requestedLocation(location)
                .resolvedLocation(range.start() == range.end()
                        ? String.valueOf(range.start())
                        : range.start() + "-" + range.end())
                .segments(segments)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long knowledgeBaseId, Long fileId, Long userId) {
        KnowledgeBase knowledgeBase = getOwnedKnowledgeBase(knowledgeBaseId, userId);
        FileDetail fileDetail = fileDetailRepository.findById(fileId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "文件不存在"));
        if (!userId.equals(fileDetail.getUserId()) || !knowledgeBaseId.equals(fileDetail.getKnowledgeBaseId())) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "无权删除该文件");
        }

        List<DocumentUnit> units = documentUnitRepository.findByFileId(fileId);
        vectorStoreRepository.deleteByIds(units.stream().map(DocumentUnit::getId).toList());
        documentUnitRepository.deleteByFileId(fileId);
        fileDetailRepository.deleteById(fileId);

        knowledgeBase.removeFile(fileDetail.getFileSize());
        int chunkCount = units.size();
        knowledgeBase.setDocumentCount(Math.max(0, knowledgeBase.getDocumentCount() - chunkCount));
        knowledgeBase.onUpdate();
        knowledgeBaseRepository.update(knowledgeBase);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = getOwnedKnowledgeBase(knowledgeBaseId, userId);
        List<FileDetail> files = fileDetailRepository.findByKnowledgeBaseId(knowledgeBaseId);
        for (FileDetail file : files) {
            deleteFile(knowledgeBaseId, file.getId(), userId);
        }
        knowledgeBaseRepository.deleteById(knowledgeBaseId);
    }

    private KnowledgeBase getOwnedKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "知识库不存在"));
        if (!userId.equals(knowledgeBase.getUserId()) || knowledgeBase.isDeleted()) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCodeEnum.FILE_IS_EMPTY, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCodeEnum.FILE_SIZE_EXCEEDED, "单个文件不能超过20MB");
        }
        resolveFileType(file.getOriginalFilename());
    }

    private FileType resolveFileType(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        FileType fileType = FileType.fromExtension(extension);
        if (fileType == null) {
            throw new BizException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED, "当前仅支持 md、txt、html、pdf 文件");
        }
        if (!(fileType == FileType.MD || fileType == FileType.MARKDOWN || fileType == FileType.TXT
                || fileType == FileType.HTML || fileType == FileType.PDF)) {
            throw new BizException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED, "当前仅支持 md、txt、html、pdf 文件");
        }
        return fileType;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAILED, "读取文件失败: " + e.getMessage());
        }
    }

    private String extractText(FileType fileType, byte[] bytes) {
        return switch (fileType) {
            case MD, MARKDOWN, TXT -> new String(bytes, StandardCharsets.UTF_8);
            case HTML -> stripHtml(new String(bytes, StandardCharsets.UTF_8));
            case PDF -> extractPdfText(bytes);
            default -> throw new BizException(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED, "当前文件类型暂不支持");
        };
    }

    private String extractPdfText(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "PDF解析失败: " + e.getMessage());
        }
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Range parseRange(String location) {
        if (!StringUtils.hasText(location)) {
            return new Range(1, 1);
        }
        Matcher matcher = LOCATION_PATTERN.matcher(location.trim());
        if (!matcher.find()) {
            return new Range(1, 1);
        }
        int start = Integer.parseInt(matcher.group(1));
        int end = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : start;
        if (end < start) {
            end = start;
        }
        return new Range(Math.max(1, start), Math.max(1, end));
    }

    private List<String> splitIntoChunks(String text) {
        String normalized = text.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            if (cleaned.length() > CHUNK_MAX_CHARS) {
                if (buffer.length() > 0) {
                    chunks.add(buffer.toString());
                    buffer = new StringBuilder();
                }
                chunks.addAll(TextChunker.splitBySentence(cleaned, CHUNK_MAX_CHARS));
                continue;
            }

            if (buffer.length() > 0 && buffer.length() + cleaned.length() + 2 > CHUNK_MAX_CHARS) {
                chunks.add(buffer.toString());
                buffer = new StringBuilder();
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(cleaned);
        }

        if (buffer.length() > 0) {
            chunks.add(buffer.toString());
        }

        return chunks.stream()
                .map(String::trim)
                .filter(chunk -> !chunk.isEmpty())
                .toList();
    }

    private String generateStorageName(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String suffix = StringUtils.hasText(extension) ? "." + extension.toLowerCase(Locale.ROOT) : "";
        return LocalDateTime.now().toString().replace(":", "-") + suffix;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private KnowledgeBaseResponse toKnowledgeBaseResponse(KnowledgeBase knowledgeBase) {
        List<FileDetail> files = fileDetailRepository.findByKnowledgeBaseId(knowledgeBase.getId());
        int documentCount = files.stream()
                .map(FileDetail::getId)
                .filter(id -> id != null)
                .mapToInt(documentUnitRepository::countByFileId)
                .sum();
        List<KnowledgeBaseFileResponse> fileResponses = files.stream()
                .map(this::toKnowledgeBaseFileResponse)
                .sorted(Comparator.comparing(KnowledgeBaseFileResponse::getCreatedAt).reversed())
                .toList();

        return KnowledgeBaseResponse.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .status(knowledgeBase.getStatus() != null ? knowledgeBase.getStatus().name() : null)
                .fileCount(files.size())
                .totalSize(files.stream().map(FileDetail::getFileSize).filter(size -> size != null).mapToLong(Long::longValue).sum())
                .documentCount(documentCount)
                .createdAt(knowledgeBase.getCreatedAt())
                .updatedAt(knowledgeBase.getUpdatedAt())
                .files(fileResponses)
                .build();
    }

    private KnowledgeBaseFileResponse toKnowledgeBaseFileResponse(FileDetail fileDetail) {
        int chunkCount = documentUnitRepository.countByFileId(fileDetail.getId());
        int vectorizedCount = documentUnitRepository.countVectorizedByFileId(fileDetail.getId());
        return KnowledgeBaseFileResponse.builder()
                .id(fileDetail.getId())
                .originalName(fileDetail.getOriginalName())
                .fileType(fileDetail.getFileType() != null ? fileDetail.getFileType().name() : null)
                .fileSize(fileDetail.getFileSize())
                .status(fileDetail.getProcessStatus() != null ? fileDetail.getProcessStatus().name() : null)
                .processProgress(fileDetail.getProcessProgress())
                .chunkCount(chunkCount)
                .vectorizedChunkCount(vectorizedCount)
                .errorMessage(fileDetail.getErrorMessage())
                .createdAt(fileDetail.getCreatedAt())
                .processedAt(fileDetail.getProcessedAt())
                .build();
    }

    private RagSearchResultDto toSearchResult(DocumentRetrievalService.RetrievalResult result) {
        return RagSearchResultDto.builder()
                .documentUnitId(result.getDocumentUnitId())
                .fileId(result.getFileId())
                .title(result.getTitle())
                .score(result.getScore())
                .content(result.getContent())
                .build();
    }

    private record Range(int start, int end) {
    }
}
