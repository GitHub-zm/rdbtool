1、调用方式
	java -jar rdbtool.jar [源文件路径] [目标csv文件保存路径] [导出KEY筛选]
	注：源文件路径-必传项；
		目标csv文件保存路径-必传项；
		导出KEY筛选-可选项，筛选包含输入内容的KEY，多个用“,”隔开；
	示例：java -jar rdbtool.jar /usr/local/dump.rdb /usr/local/
		  java -jar rdbtool.jar /usr/local/dump.rdb /usr/local/ key1,key2
		  
2、返回说明
	errCd：0-成功，此时results为转换总行数，fileCount为生产文件个数
	errCd：1000-失败，未传入参数或入参不完整,调用失败
	errCd：1002-失败，rdb文件不存在
	errCd：9002-失败，生成csv文件异常
	errCd：9001-失败，rdb文件解析异常


3、生成csv文件名称说明：由于csv文件单页可保存最大数据行数约为104万行，因此当数据量多大时，会产生多个csv文件，规则如下：源文件名称_文件序号.csv
	例如：源文件名称为：dump.rdb
		  生成文件名称为：dump.rdb_1.csv\dump.rdb_2.csv\dump.rdb_3.csv\...



















































