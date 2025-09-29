<?php
/**
 * NexusVoice phpMyAdmin 自定义配置
 * 针对 MySQL 8.0 和 NexusVoice 项目优化
 */

// 基础配置
$cfg['blowfish_secret'] = 'nexusvoice-phpmyadmin-secret-key-' . hash('sha256', 'nexusvoice');
$cfg['DefaultLang'] = 'zh_CN';
$cfg['ServerDefault'] = 1;

// 安全配置
$cfg['LoginCookieValidity'] = 3600 * 8; // 8小时
$cfg['LoginCookieRecall'] = true;
$cfg['LoginCookieStore'] = 3600 * 24; // 24小时
$cfg['ShowPhpInfo'] = false;
$cfg['ShowServerInfo'] = false;
$cfg['VersionCheck'] = false;

// 上传配置
$cfg['UploadDir'] = '/tmp';
$cfg['SaveDir'] = '/tmp';
$cfg['MaxSizeForInputField'] = 50 * 1024 * 1024; // 50MB

// 导入导出配置
$cfg['Import']['format'] = 'sql';
$cfg['Import']['charset'] = 'utf-8';
$cfg['Import']['sql_compatibility'] = 'NONE';
$cfg['Import']['sql_no_auto_value_on_zero'] = true;
$cfg['Import']['sql_read_as_multibytes'] = false;

$cfg['Export']['format'] = 'sql';
$cfg['Export']['compression'] = 'gzip';
$cfg['Export']['charset'] = 'utf-8';
$cfg['Export']['sql_compatibility'] = 'NONE';
$cfg['Export']['sql_structure_or_data'] = 'structure_and_data';

// SQL 查询配置
$cfg['SQLQuery']['Edit'] = true;
$cfg['SQLQuery']['Explain'] = true;
$cfg['SQLQuery']['ShowAsPHP'] = true;
$cfg['SQLQuery']['Validate'] = true;
$cfg['SQLQuery']['Refresh'] = true;

// 界面配置
$cfg['ThemeDefault'] = 'pmahomme';
$cfg['DefaultTabServer'] = 'main.php';
$cfg['DefaultTabDatabase'] = 'structure.php';
$cfg['DefaultTabTable'] = 'browse.php';

// 浏览配置
$cfg['MaxRows'] = 50;
$cfg['Order'] = 'ASC';
$cfg['DisplayServersList'] = false;
$cfg['DisplayDatabasesList'] = true;
$cfg['ShowStats'] = true;
$cfg['ShowServerInfo'] = false;
$cfg['ShowPhpInfo'] = false;
$cfg['ShowChgPassword'] = false;

// 编辑配置
$cfg['ProtectBinary'] = 'blob';
$cfg['ShowFunctionFields'] = true;
$cfg['ShowFieldTypesInDataEditView'] = true;
$cfg['InsertRows'] = 2;
$cfg['ForeignKeyMaxLimit'] = 100;

// 数据库配置（针对 NexusVoice 项目）
$cfg['Servers'][1]['host'] = 'nexusvoice-mysql';
$cfg['Servers'][1]['port'] = '3306';
$cfg['Servers'][1]['socket'] = '';
$cfg['Servers'][1]['connect_type'] = 'tcp';
$cfg['Servers'][1]['extension'] = 'mysqli';
$cfg['Servers'][1]['auth_type'] = 'cookie';
$cfg['Servers'][1]['user'] = '';
$cfg['Servers'][1]['password'] = '';
$cfg['Servers'][1]['AllowNoPassword'] = false;
$cfg['Servers'][1]['AllowRoot'] = true;

// 高级功能配置（需要 phpmyadmin 数据库）
$cfg['Servers'][1]['pmadb'] = 'phpmyadmin';
$cfg['Servers'][1]['bookmarktable'] = 'pma__bookmark';
$cfg['Servers'][1]['relation'] = 'pma__relation';
$cfg['Servers'][1]['table_info'] = 'pma__table_info';
$cfg['Servers'][1]['table_coords'] = 'pma__table_coords';
$cfg['Servers'][1]['pdf_pages'] = 'pma__pdf_pages';
$cfg['Servers'][1]['column_info'] = 'pma__column_info';
$cfg['Servers'][1]['history'] = 'pma__history';
$cfg['Servers'][1]['table_uiprefs'] = 'pma__table_uiprefs';
$cfg['Servers'][1]['tracking'] = 'pma__tracking';
$cfg['Servers'][1]['userconfig'] = 'pma__userconfig';
$cfg['Servers'][1]['recent'] = 'pma__recent';
$cfg['Servers'][1]['favorite'] = 'pma__favorite';
$cfg['Servers'][1]['users'] = 'pma__users';
$cfg['Servers'][1]['usergroups'] = 'pma__usergroups';
$cfg['Servers'][1]['navigationhiding'] = 'pma__navigationhiding';
$cfg['Servers'][1]['savedsearches'] = 'pma__savedsearches';
$cfg['Servers'][1]['central_columns'] = 'pma__central_columns';
$cfg['Servers'][1]['designer_settings'] = 'pma__designer_settings';
$cfg['Servers'][1]['export_templates'] = 'pma__export_templates';

// NexusVoice 特定配置
$cfg['DefaultConnectionCollation'] = 'utf8mb4_unicode_ci';

// 自定义 CSS
$cfg['ThemePerServer'] = false;

// 性能优化
$cfg['MemoryLimit'] = '512M';
$cfg['ExecTimeLimit'] = 600;

// 导航配置
$cfg['NavigationTreePointerEnable'] = true;
$cfg['NavigationTreeDisplayLogo'] = false;
$cfg['NavigationTreeDisplayServers'] = false;
$cfg['NavigationTreeDefaultTabTable'] = 'structure';
$cfg['NavigationTreeDefaultTabTable2'] = '';
$cfg['NavigationTreeEnableGrouping'] = true;
$cfg['NavigationTreeDbSeparator'] = '_';
$cfg['NavigationTreeTableSeparator'] = '__';
$cfg['NavigationTreeTableLevel'] = 1;

// 控制台配置
$cfg['Console']['StartHistory'] = true;
$cfg['Console']['AlwaysExpand'] = false;
$cfg['Console']['CurrentQuery'] = true;
$cfg['Console']['EnterExecutes'] = false;
$cfg['Console']['DarkTheme'] = false;
$cfg['Console']['Mode'] = 'collapse';
$cfg['Console']['Height'] = 92;
$cfg['Console']['GroupQueries'] = false;
$cfg['Console']['OrderBy'] = 'exec';
$cfg['Console']['Order'] = 'asc';

// 字体配置
$cfg['DefaultCharset'] = 'utf-8';
$cfg['AllowArbitraryServer'] = false;

// 错误报告
$cfg['SendErrorReports'] = 'never';
$cfg['ConsoleEnterExecutes'] = false;
?>
