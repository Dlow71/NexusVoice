#!/bin/bash
# NexusVoice Backend - Docker 一键启动脚本
# 作者：NexusVoice Team
# 版本：1.0.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# 项目信息
PROJECT_NAME="NexusVoice Backend"
PROJECT_VERSION="1.0.0"

# 打印标题
print_title() {
    echo -e "${CYAN}"
    echo "=========================================="
    echo "  $PROJECT_NAME v$PROJECT_VERSION"
    echo "  Docker 容器化部署脚本"
    echo "=========================================="
    echo -e "${NC}"
}

# 打印信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印警告
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 打印错误
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印成功
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 命令未找到，请先安装 $1"
        exit 1
    fi
}

# 检查文件是否存在
check_file() {
    if [ ! -f "$1" ]; then
        print_error "文件 $1 不存在"
        return 1
    fi
    return 0
}

# 检查环境变量文件
check_env_file() {
    if [ ! -f ".env" ]; then
        print_warning ".env 文件不存在"
        if [ -f ".env.example" ]; then
            print_info "发现 .env.example 文件，正在复制为 .env"
            cp .env.example .env
            print_warning "请编辑 .env 文件，配置必要的环境变量（如API密钥等）"
            read -p "是否现在编辑 .env 文件？(y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                ${EDITOR:-nano} .env
            fi
        else
            print_error ".env.example 文件也不存在，无法创建环境配置"
            exit 1
        fi
    else
        print_success ".env 文件已存在"
    fi
}

# 检查Docker环境
check_docker() {
    print_info "检查 Docker 环境..."

    check_command "docker"

    # 优先使用 docker compose，其次 docker-compose
    if docker compose version >/dev/null 2>&1; then
        DC="docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        DC="docker-compose"
    else
        print_error "未检测到 docker compose 或 docker-compose，请安装其中之一"
        exit 1
    fi

    # 检查Docker是否运行
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker 服务未运行，请先启动 Docker"
        exit 1
    fi

    print_success "Docker 环境检查通过（使用: $DC）"
}

# 清理旧的容器和镜像
cleanup() {
    print_info "清理旧的容器和镜像..."
    
    # 停止并删除容器
    "$DC" down --remove-orphans 2>/dev/null || true
    
    # 删除悬空镜像
    docker image prune -f >/dev/null 2>&1 || true
    
    print_success "清理完成"
}

# 创建必要的目录
create_directories() {
    print_info "创建必要的目录..."
    
    local dirs=(
        "logs"
        "uploads"
        "docker/mysql/logs"
        "docker/mysql/backup"
        "docker/redis/data"
    )
    
    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_info "创建目录: $dir"
        fi
    done
    
    print_success "目录创建完成"
}

# 构建和启动服务
start_services() {
    print_info "构建和启动服务..."
    
    # 检查是否需要构建
    if [ "$1" = "--build" ] || [ "$1" = "-b" ]; then
        print_info "重新构建 Docker 镜像..."
        "$DC" build --no-cache
    fi
    
    # 启动服务
    print_info "启动所有服务..."
    "$DC" up -d
    
    print_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    print_info "等待服务启动完成..."
    
    local max_attempts=60
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        # 检查MySQL
        if "$DC" exec -T nexusvoice-mysql mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD:-root123} --silent 2>/dev/null; then
            print_success "MySQL 服务已就绪"
            break
        fi
        
        attempt=$((attempt + 1))
        if [ $attempt -eq $max_attempts ]; then
            print_warning "MySQL 服务启动超时，但这可能是正常的"
            break
        fi
        
        echo -n "."
        sleep 2
    done
    
    echo ""
    
    # 等待应用服务
    print_info "等待应用服务启动..."
    sleep 10
    
    # 检查应用健康状态
    local app_attempts=30
    local app_attempt=0
    
    while [ $app_attempt -lt $app_attempts ]; do
        if curl -f http://localhost:${APP_PORT:-8080}/actuator/health >/dev/null 2>&1; then
            print_success "应用服务已就绪"
            break
        fi
        
        app_attempt=$((app_attempt + 1))
        if [ $app_attempt -eq $app_attempts ]; then
            print_warning "应用服务健康检查超时，请查看日志"
            break
        fi
        
        echo -n "."
        sleep 3
    done
    
    echo ""
}

# 显示服务状态
show_status() {
    print_info "服务状态："
    "$DC" ps
    
    echo ""
    print_info "服务访问地址："
    echo -e "  ${GREEN}• 应用服务:${NC} http://localhost:${APP_PORT:-8080}"
    echo -e "  ${GREEN}• API 文档:${NC} http://localhost:${APP_PORT:-8080}/swagger-ui.html"
    echo -e "  ${GREEN}• 健康检查:${NC} http://localhost:${APP_PORT:-8080}/actuator/health"
    echo -e "  ${GREEN}• Druid 监控:${NC} http://localhost:${APP_PORT:-8080}/druid"
    echo -e "  ${GREEN}• phpMyAdmin:${NC} http://localhost:${PHPMYADMIN_PORT:-8082}"
    
    if "$DC" ps | grep -q redis-commander; then
        echo -e "  ${GREEN}• Redis 管理:${NC} http://localhost:${REDIS_COMMANDER_PORT:-8083}"
    fi
}

# 显示日志
show_logs() {
    local service=${1:-nexusvoice-backend}
    print_info "显示 $service 服务日志（按 Ctrl+C 退出）："
    "$DC" logs -f $service
}

# 进入容器
enter_container() {
    local service=${1:-nexusvoice-backend}
    print_info "进入 $service 容器："
    # 优先尝试 bash，不存在则回退到 sh
    if "$DC" exec -T $service /bin/bash -lc 'exit 0' 2>/dev/null; then
        "$DC" exec $service /bin/bash
    else
        "$DC" exec $service /bin/sh
    fi
}

# 停止服务
stop_services() {
    print_info "停止所有服务..."
    "$DC" down
    print_success "服务已停止"
}

# 重启服务
restart_services() {
    print_info "重启所有服务..."
    "$DC" restart
    print_success "服务已重启"
}

# 显示帮助信息
show_help() {
    echo -e "${WHITE}用法:${NC}"
    echo "  $0 [选项] [命令] [参数]"
    echo ""
    echo -e "${WHITE}选项:${NC}"
    echo "  -b, --build     重新构建镜像"
    echo "  -h, --help      显示帮助信息"
    echo ""
    echo -e "${WHITE}命令:${NC}"
    echo "  start           启动所有服务（默认）"
    echo "  stop            停止所有服务"
    echo "  restart         重启所有服务"
    echo "  status          显示服务状态"
    echo "  logs [service]  显示服务日志"
    echo "  exec [service]  进入服务容器"
    echo "  cleanup         清理容器和镜像"
    echo ""
    echo -e "${WHITE}示例:${NC}"
    echo "  $0                    # 启动所有服务"
    echo "  $0 --build            # 重新构建并启动"
    echo "  $0 stop               # 停止所有服务"
    echo "  $0 logs               # 查看应用日志"
    echo "  $0 logs mysql         # 查看MySQL日志"
    echo "  $0 exec               # 进入应用容器"
}

# 主函数
main() {
    print_title
    
    # 解析参数
    local build_flag=""
    local command="start"
    local service=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -b|--build)
                build_flag="--build"
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            start|stop|restart|status|logs|exec|cleanup)
                command=$1
                shift
                if [ "$command" = "logs" ] || [ "$command" = "exec" ]; then
                    service=${1:-nexusvoice-backend}
                    [ $# -gt 0 ] && shift
                fi
                ;;
            *)
                print_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 执行对应命令
    case $command in
        start)
            check_docker
            check_env_file
            create_directories
            if [ "$build_flag" = "--build" ]; then
                cleanup
            fi
            start_services $build_flag
            wait_for_services
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs $service
            ;;
        exec)
            enter_container $service
            ;;
        cleanup)
            cleanup
            ;;
        *)
            print_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 错误处理
trap 'print_error "脚本执行出错，退出码: $?"' ERR

# 执行主函数
main "$@"
