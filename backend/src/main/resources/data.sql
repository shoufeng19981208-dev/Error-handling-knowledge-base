-- 报错处理知识库初始数据
-- 来源：TO层报错处理方案.docx、数据交换平台常见报错解决方案.docx

-- TO层报错处理方案
INSERT INTO error_record (id, error_title, error_content, error_screenshot, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(1, 'DataStage轮询头作业报错', 'DataStage轮询头作业（以batch结尾）报错，源系统的结束标识数据未生成好。', NULL, '1. 确认调度名是否为 inf_*_batch*；2. 确认是轮询作业（batch结尾）还是定时作业（time结尾）；3. 轮询作业报错都是源系统结束标识数据未生成好，联系源系统处理好后重跑头作业即可。', 'TO层报错', 'RECORDED', 'DataStage,头作业,batch,轮询,调度', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(2, 'OSS取文件模块OK报错', 'OSS取文件轮询模块ok文件失败，调度名涉及 ext_ifl_*_ready 等系统，使用 get_ready_oss.sh 轮询。', '1. 确认调度名是否包含 ext_*_ready；2. 查看使用的shell是否为 get_ready_oss.sh；3. 确认是轮询模块OK文件失败，联系源系统说明模块ok文件未生成好，等源系统处理好后重跑调度。', 'TO层报错', 'RECORDED', 'OSS,取文件,模块OK,ready,get_ready_oss', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(3, 'OSS取文件单表OK报错', 'OSS取文件单表OK轮询失败，使用 get_file_oss_ok.sh 的调度，涉及 ext_ifl、ext_ifo、ext_cfc 等系统。', '1. 查看跑批命令是否使用的 get_file_oss_ok.sh；2. 确认是轮询单表OK文件失败；3. 联系源系统说明单表ok文件未生成好，等源系统处理好后重跑调度。', 'TO层报错', 'RECORDED', 'OSS,单表OK,get_file_oss_ok,取文件', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(4, 'FTP取文件失败', 'FTP取文件调度报错，涉及 ext_crd（零售条线）、ext_hrs（运营条线），使用shell取文件并改名。', '1. 确认调度名是否为 ext_crd* 或 ext_hrs*；2. 确认是文件未取到；3. 联系源系统确认文件是否已放到FTP目录，处理好后重跑调度。', 'TO层报错', 'RECORDED', 'FTP,取文件,ext_crd,ext_hrs', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(5, 'DataStage作业未导入', 'DataStage作业未导入，导致调度无法找到作业。', '1. 在DataStage客户端重新导入ds作业；2. 编译作业后重跑调度。', 'TO层报错', 'RECORDED', 'DataStage,未导入,作业导入,编译', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(6, 'DataStage作业未编译', 'DataStage作业未编译导致跑批失败。', '1. 在DataStage客户端找到对应作业；2. 打开作业重新编译；3. 重跑调度。', 'TO层报错', 'RECORDED', 'DataStage,未编译,编译,作业', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(7, 'Oracle数据库连不上', 'DataStage抽数时Oracle数据库连接失败，可能原因：配置信息不正确、网络不通、用户名密码错误。', '1. 登录服务器查看 ext_config.conf：cd /home/edwuser/edwscript/etc，vi ext_config.conf；2. 查看 passwd.conf 获取密码；3. 解密密码：python $ETL_HOME/package/utils/encryptutils.py dec 密文；4. 查看 tnsnames.ora 获取IP和实例名；5. 使用 sqlplus 用户名/密码@ip/实例名 测试连接；6. 如果不通则填网络打通申请单，如用户名密码错误则找源系统要新信息。', 'TO层报错', 'RECORDED', 'Oracle,连接失败,sqlplus,ext_config,passwd.conf,tnsnames', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(8, '源系统表不存在', 'DataStage抽数时提示源系统表不存在。', '1. 使用 sqlplus 用户名/密码@ip/实例名 连接源系统数据库；2. 执行 desc schema.源系统表名 确认表是否存在；3. 确认不存在后联系源系统处理。', 'TO层报错', 'RECORDED', '表不存在,desc,sqlplus,源系统', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(9, '源系统字段不存在', 'DataStage抽数时提示源系统表的某个字段不存在。', '1. 使用 sqlplus 用户名/密码@ip/实例名 连接源系统数据库；2. 执行 select 不存在字段 from schema.源系统表名 where rownum < 1 确认字段是否存在；3. 确认不存在后联系源系统处理。', 'TO层报错', 'RECORDED', '字段不存在,sqlplus,源系统', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(10, 'DataStage空间满报错', 'DataStage抽数时空间满了导致作业失败。', '1. 删空间；2. 重新抽数。', 'TO层报错', 'RECORDED', '空间满,磁盘空间,DataStage', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(11, '截取日志爆满报错', '源系统字段扩长但未通知，DataStage抽数varchar长度比源系统短，每条数据产生截取警告日志导致日志爆满。', '1. 找到不一致的字段（报错信息中有该字段）；2. 对比源系统和数仓表结构；3. 将数仓DataStage（两台机器）、ddl、dml、gp库表的该字段扩长；4. 清空日志：su - dsadm -> uvsh -> LOGTO edw_aas -> CLEAR.FILE RT_LOG18（两台机器都要清）；5. 重跑调度。', 'TO层报错', 'RECORDED', '截取日志,日志爆满,字段扩长,CLEAR.FILE,psql', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(12, 'ORA-01555快照过旧', 'Message: ORA-01555: snapshot too old。抽数时间段内源系统表数据更新了，导致快照过旧报错。', '重跑调度即可。', 'TO层报错', 'RECORDED', 'ORA-01555,snapshot too old,快照过旧,重跑', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(13, '0x00特殊字符报错', '数据文件存在不可见0x00特殊字符导致抽数失败。', '1. 通过shell去掉0x00特殊字符：sh /home/edwuser/edwscript/script/shell/replace_0x00.sh 20221231 itl_pms_dxloanbal；2. 重跑T层；3. 处理完后需在生产T层调度跑批命令处加上该shell语句。', 'TO层报错', 'RECORDED', '0x00,特殊字符,replace_0x00,shell', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(14, '数仓默认分隔符^O冲突', '数据中包含数仓默认分隔符^O导致解析异常。DataStage抽数需添加replace逻辑。', '1. 登录DataStage客户端对应系统（生产host: pkedwpap01p/pkedwpap02p，用户密码: isadmin/isadmin）；2. 找到对应作业打开sql_in；3. 加上去分隔符逻辑：replace(replace(replace(字段中文名,chr(10),''''),chr(13),''''),chr(15),'''')；4. 重新编译跑批；5. 两台DataStage都需要修改，编译完后关闭作业窗口防止锁作业；6. 如遇到作业被锁：su - dsadm -> uvsh -> LOGTO UV -> LIST.READU -> UNLOCK USER 33088 ALL。', 'TO层报错', 'RECORDED', '分隔符,^O,chr(15),replace,DataStage,作业锁定,UNLOCK', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(15, '回车换行missing date报错', '出现missing date报错，基本是字段数据有回车换行。报错字段的上一个字段有回车换行。', 'DataStage处理：1. 登录DataStage客户端对应系统（host: pkedwpap01p/pkedwpap02p，isadmin/isadmin）；2. 找到对应作业打开sql_in；3. 对报错的上一个字段加上去回车换行逻辑：replace(replace(字段中文名,chr(10),''''),chr(13),'''')；4. 重新编译跑批；5. 两台机器都修改，编译完后关闭作业窗口。推文件处理：联系推文件源系统，哪个字段有回车换行叫他们处理重新推文件。', 'TO层报错', 'RECORDED', '回车换行,missing date,chr(10),chr(13),replace', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(16, '多字段报错', 'DataStage出现多字段报错。推文件字段多了也会报多字段。注意：数据本身包含数仓默认分隔符也会报多字段。', '1. DataStage一般不会多字段，除非开发有问题；2. 推文件字段多了叫源系统重新推文件；3. 如果分隔符导致的多字段，参考分隔符处理方案。', 'TO层报错', 'RECORDED', '多字段,字段不一致,分隔符', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(17, '字段类型不一致报错', '源系统数值型/varchar类型字段扩长，与数仓表结构不一致。', '1. 定位到字段类型不一致的字段；2. 与源系统确认；3. 手动将ds、dml/itl、gp TO表字段扩长；4. 虚拟机维护到位。', 'TO层报错', 'RECORDED', '字段类型,扩长,varchar,数值型', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(18, '主键重复报错', 'DataStage抽数时主键重复导致作业失败。', '1. 找到重复语句；2. 跟源系统沟通是脏数据还是有用数据；3. 脏数据让源系统删除重新抽数；4. 有用数据需要重新定主键开发。', 'TO层报错', 'RECORDED', '主键重复,脏数据,主键开发', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(19, '404找不到数据文件', 'DataStage抽数时提示404，没有找到数据文件。', '用跑批命令去找数据文件，多出现在互金文件中。', 'TO层报错', 'RECORDED', '404,数据文件,互金文件', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(20, 'GTP推文件报错', 'GTP有集成到数据交换平台（可使用调度配置依赖），但是存量都没有合并到交换平台。GTP定时放文件到数仓NAS盘。', 'GTP是分开的，如有报错需详细查看报错日志。', 'TO层报错', 'RECORDED', 'GTP,推文件,NAS盘,数据交换平台', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

-- 数据交换平台常见报错解决方案
INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(21, 'Impala数据源同步无目标schema', 'Impala数据源同步没有目标schema（只有default）。', 'schema无权限（未申请下来），申请对应schema权限即可。', '数据交换平台', 'RECORDED', 'Impala,数据源同步,schema,default,权限', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(22, 'Oracle数据源连接失败 ORA-12505', 'Oracle数据源连接失败，报错 error: ORA-12505, TNS:listener does not currently know of SID given in connect descriptor。', 'JDBC连接串使用SID和SERVICE NAME时格式不同，可将连接地址改为 jdbc:oracle:thin:@<host>:<port>/<database> 尝试。', '数据交换平台', 'RECORDED', 'ORA-12505,Oracle,数据源连接,JDBC,SID,SERVICE_NAME', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(23, 'GTP节点连接失败', 'GTP连不上，GTP节点 gtp_stat -node 查看配置。', '检查端口是10252还是10254，telnet ip port 测试连通性。', '数据交换平台', 'RECORDED', 'GTP,连接失败,gtp_stat,telnet,端口', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(24, 'HDFS kinit认证失败 / ICMP Port Unreachable', 'HDFS kinit失败或代码报错 ICMP Port Unreachable。', '系统管理员操作：1. /etc/hosts 添加映射；2. /etc/krb5.conf 更新；3. 用到的krb5.conf更新（CDH认证更新，交换相应操作）。', '数据交换平台', 'RECORDED', 'HDFS,kinit,kerberos,ICMP,Port Unreachable,krb5.conf', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(25, 'Impala/Hive作业认证失败', '运行Impala、Hive数据源相关作业时，作业运行失败，报错日志提示认证失败。', '要么没放keytab，要么keytab文件有问题，用kinit试试认证是否正常。', '数据交换平台', 'RECORDED', 'Impala,Hive,认证失败,keytab,kinit', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(26, '脏数据/字段数量不一致', '脏数据报错，源文件某行有A列但尝试读取第B列，即字段数量与界面配置不一致。', '1. 确认源数据文件字段数量与界面配置的取文件入库作业字段数量一致；2. 给采集字段的第A个字段设置datax函数替换换行符。', '数据交换平台', 'RECORDED', '脏数据,字段数量,datax,换行符', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(27, '分区字段与目标表不匹配', '报错：Could not resolve column/field reference: part_num。源表无分区字段，目标表有分区字段导致不匹配。', '1. 将part_num设置为分区字段；2. 加载字段取消选择part_num。或者：1. 修改后置SQL手动为part_num赋值：insert overwrite table table_name1 partition(part_num) select columns, cast(regexp_replace(substr(etl_dt,1,10),''-'','''') from table_name2；2. 或改库对库为抽取入库，目标表多出来的字段赋常量。', '数据交换平台', 'RECORDED', '分区字段,part_num,column reference,目标表', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(28, 'HDFS写入权限拒绝', '抽数失败：permission denied: user=dtes_usr, Access=WRITE, inode=''/'': hdfs:supergroup:drwxr-xr-x。发布/抽取数据到交换HDFS无权限。', '存储路径缓存问题。编辑作业，重新选择nas/hdfs（或重新选择采集工具），保存，运行。', '数据交换平台', 'RECORDED', 'HDFS,权限拒绝,permission denied,dtes_usr', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(29, '编码格式错误 invalid byte sequence', '报错：ERROR: invalid byte sequence for encoding "UTF8": 0xd5 0xc5。', '抽取/取文件的编码问题（gbk/utf8），调整编码格式。', '数据交换平台', 'RECORDED', '编码,UTF8,gbk,invalid byte sequence', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(30, 'Oracle标识符无效 ORA-00904', '报错：java.sql.SQLSyntaxErrorException: ORA-00904: "TO_DATE": 标识符无效。Oracle字段为小写，查询语句自动转成大写查询。', 'Oracle严格区分大小写，修改源数据字段名为大写。', '数据交换平台', 'RECORDED', 'ORA-00904,Oracle,标识符无效,大小写,TO_DATE', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(31, '旧作业展示为空', '数据源同步后识别到表删除，该表相关交换作业查询不到信息。', '重新建表，交换数据源同步，旧作业信息即可查询到。', '数据交换平台', 'RECORDED', '旧作业,作业为空,数据源同步,表删除', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(32, 'PXF卸数失败无权限', 'PXF卸数失败，提示无权限建外表。', '系统管理员操作：pxf的外表权限赋给dmrdm_usr。', '数据交换平台', 'RECORDED', 'PXF,卸数,无权限,外表,dmrdm_usr', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(33, 'PXF协议不存在', '报错：ERROR: protocol "pxf" does not exist。GP切库，PXF每季度需要重启。', '1. 登录 159.1.41.222（gpadmin/gpadmin）；2. 参考SVN文档重启PXF：svn://159.1.65.75/南京银行数据平台项目群/07-平台开发组/17-数据交换系统建设项目/06第三方工具相关材料/PXF/安装PXF全过程.txt；3. 或联系谢玉宝。', '数据交换平台', 'RECORDED', 'PXF,protocol does not exist,重启,pxf', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(34, 'GPFDIST卸数失败空间不足', 'GPFDIST卸数失败，http response code 500 from gpfdist。gpsql日志显示 No space left on device。', '建外表空间不足，先清NAS空间试试（建外表可能在NAS）。', '数据交换平台', 'RECORDED', 'GPFDIST,卸数,500,No space left,空间不足', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(35, 'GPFDIST未正常启动', '运行GP数据源且采集/加载工具为GPFDIST的相关作业失败，提示 ERROR: error when writing data to gpfdist。', '系统管理员操作：检查gpfdist是否正常启动：ps -ef | grep gpfdist。如未启动则重新启动。', '数据交换平台', 'RECORDED', 'GPFDIST,未启动,ps,error writing data', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(36, 'GPFDIST psql未找到命令', 'GP卸数失败，提示 gpfdist脚本执行异常：xxx psql：未找到命令。', '系统管理员操作：检查Executor的 application.properties 中的 gpfdist.lib.path 和 gpfdist.bin.path 路径配置是否正确。', '数据交换平台', 'RECORDED', 'GPFDIST,psql,未找到命令,路径配置', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(37, 'DATAX写入效率慢', 'DATAX写入效率慢，可能表有索引影响写入速率。', '大数据不需要索引。可在前置SQL删除索引，后置SQL新增索引。', '数据交换平台', 'RECORDED', 'DATAX,写入效率,索引,前置SQL,后置SQL', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(38, 'DATAX提示缺失表达式', 'DATAX运行提示缺失表达式。', '字段中包含数据库关键字/敏感字符，检查字段名并处理。', '数据交换平台', 'RECORDED', 'DATAX,缺失表达式,关键字,敏感字符', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(39, 'GTP文件传输失败', '运行订阅-交换到文件GTP文件传输作业失败。', '1. 检查gtp节点文件路径；2. 系统管理员查看updir、downdir是否存在并重配；3. 在gtp安装目录执行：. setp -> gtp -b。cd /home/dtes_usr/client/gtp/TongGTP6.1 -> . setp -> gtp -b。', '数据交换平台', 'RECORDED', 'GTP,文件传输,updir,downdir,setp', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(40, 'GTP License Version error', 'GTP无法启动，提示 GTP check license error: License Version error！', '安装目录下的 license.dat 有问题，从别的服务器复制一份过来，停止、启动、编译gtp。', '数据交换平台', 'RECORDED', 'GTP,License,版本错误,license.dat', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(41, 'GTP uploadDir failed errno=-6261', 'GTP报错：gtp_uploadDir failed, errno=[-6261]。', '目标节点文件路径要以/UpDir参数开头。1. 查看安装GTP的节点错误代码：cd /home/dtes_usr/client/gtp/TongGTP6.1 -> vim gtp_errormsg.xml；2. 查看DownDir、UpDir参数路径：cat /home/dtes_usr/client/gtp/TongGTP6.1/etc/gtp_client_broker.conf。', '数据交换平台', 'RECORDED', 'GTP,uploadDir,-6261,UpDir,路径', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(42, 'CTM作业名称长度超限', '报错：File Name - Error: EM50004E Maximum length for that value should be 0-64。CTM作业名称长度限制64字符。', 'CTM作业长度限制64字符，交换会对作业名称增加前后缀（-end后缀），应修改作业名称长度。', '数据交换平台', 'RECORDED', 'CTM,EM50004E,名称长度,作业名称', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(43, 'CTM同步失败', '报错：以下装数任务同步失败:CTM作业同步失败。', '检查ctm日志：tail -200f /home/weblogic/sunline/ctm/ctm.log。可能原因：a) 管理端ctm服务挂了，执行 sh /home/weblogic/sunline/ctm/restart.sh 重启；b) folder定义失败，交换批次前缀必须与application相同；c) 前置条件名称前多了空格。', '数据交换平台', 'RECORDED', 'CTM,同步失败,ctm.log,重启,folder', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(44, '前端导出部署包超时', '前端导出部署包时间过长，F12提示 gateway time out。', '系统管理员操作：修改 nginx.conf 配置文件的 keep_alive_timeout 属性值改大（默认60s），修改后重启nginx。', '数据交换平台', 'RECORDED', '部署包,gateway time out,nginx,keep_alive_timeout', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(45, '部署包导入唯一标识冲突', '部署包导入失败，提示 A different object with the same identifier value was already associated with the session：字段带租户编号。', '重建数据源，重建作业。', '数据交换平台', 'RECORDED', '部署包导入,唯一标识冲突,session,租户编号', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(46, '部署失败 could not execute batch', '部署失败，报错 could not execute batch。', '通过后台日志定位报错代码，一般为数据库字段长度不够。执行：alter table DD_DBTODB_FIELD modify (s_field_alias varchar2(2000))。', '数据交换平台', 'RECORDED', '部署失败,could not execute batch,字段长度,alter table', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(47, '界面查询装数定义异常', '查看或编辑作业界面报错：查询装数定义[id]异常，详情见后台日志信息。', '1. 元数据同步：目标表被删除后元数据同步会删除所有版本，重新建表同步后版本号为1；2. 新建作业重新选择目标表（版本1）。', '数据交换平台', 'RECORDED', '界面报错,装数定义,元数据同步,版本号', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(48, '删除作业后仍显示重名', '已删除对应作业名但依旧显示存在重名作业。删除发布作业最新版本后，未更新发布清单表历史版本的is_latest字段。', '1. 通过发布作业表查询作业ID：select dqd.id, dqd.name from de_query_define dqd where job_type=''DES'' and name like ''%作业名%''；2. 根据job_id查询发布清单表：select bpfi.id, bpfi.job_id, bpfi.is_latest from BDIP_PUB_FILE_INFO bpfi where job_id in (''作业ID'') for update；3. 手动改is_latest字段为''Y''。', '数据交换平台', 'RECORDED', '重名,删除作业,is_latest,de_query_define,BDIP_PUB_FILE_INFO', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(49, 'ORA-01653表空间不足', '报错：ERROR: ORA-01653: 表 DTES_USR.PF_OPERATIONLOG 无法通过128(在表空间 USERS 中)扩展。', '1. 查看USER表空间使用率：select a.TABLESPACE_NAME, total/(1024*1024), free/(1024*1024), round((total-free)/total,4)*100 from (select tablespace_name, sum(bytes) free from dba_free_space group by tablespace_name) a, (SELECT tablespace_name, sum(bytes) total from dba_data_files group by tablespace_name) b where a.TABLESPACE_NAME = b.TABLESPACE_NAME；2. 检查是否自动扩展：select file_name,tablespace_name,autoextensible from dba_data_files where tablespace_name=''USERS''；3. 增加自动扩展表空间文件：alter tablespace USERS add datafile ''/home/oracle/app/oracle/oradata/DTEXSYS/users05.dbf'' size 1000M autoextend on next 500M MAXSIZE unlimited。', '数据交换平台', 'RECORDED', 'ORA-01653,表空间,USERS,扩展,alter tablespace', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(50, 'GP权限不足', '报错：permission denied: no privilege to create a readable gpfdist(s) external table。GP用户无权限创建只读外表。', '联系管理员赋权。', '数据交换平台', 'RECORDED', 'GP,权限不足,gpfdist,外表,permission denied', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(51, 'PXF权限拒绝', '报错：permission denied for external protocol pxf。GP用户无权限使用PXF建外部表。', 'UAT环境联系交换平台负责人赋权，生产环境联系谢玉宝(13739186551)。', '数据交换平台', 'RECORDED', 'PXF,权限拒绝,permission denied,赋权', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(52, 'coordinator运行超时', '报错：coordinator could not finished before job timeout。运行超时。', '联系交换平台负责人修改限制运行时间。', '数据交换平台', 'RECORDED', 'coordinator,超时,job timeout,运行时间', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(53, '分区表异常', '报错：Find table without partition yet partition_date existed, abort. 非分区表但选择了分区字段。', '检查表是否为分区表，非分区表去掉分区字段选择。', '数据交换平台', 'RECORDED', '分区表,partition_date,abort,非分区表', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

INSERT INTO error_record (id, error_title, error_content, solution_steps, category, status, keywords, registrar, register_time, updater, update_time) VALUES
(54, '分区异常', '报错：Unexpected exception: Partition。分区异常。', '检查重跑分区是否存在。', '数据交换平台', 'RECORDED', 'Partition,Unexpected exception,分区异常,重跑', '知识库初始化', '2024-01-01 00:00:00', '知识库初始化', '2024-01-01 00:00:00');

-- 重置自增序列，避免后续新增记录时ID冲突
ALTER TABLE error_record ALTER COLUMN id RESTART WITH 100;
