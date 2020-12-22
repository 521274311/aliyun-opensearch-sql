# aliyun-opensearch-sql
### 提供阿里云开放搜索SQL支持

#### SQL使用语法：
select  
&ensp;&ensp;&ensp;&ensp;[Distinct打散]  
&ensp;&ensp;&ensp;&ensp;[aggregate(count,sum,max,min)(聚合)]  
&ensp;&ensp;&ensp;&ensp;[field(字段)..]  
from [appName1],[appName2],..  
where  
&ensp;&ensp;&ensp;&ensp;([index(索引)[=|like]character] [..OR|AND])  
&ensp;&ensp;&ensp;&ensp;[..AND|OR]  
&ensp;&ensp;&ensp;&ensp;([index(索引)[=|>|>=|<|<=]number] [..OR|AND])  
group by [field]  
order by [field] [asc|desc]..  
limit min,count  

#### 当前版本：v0.0.1-SNAPSHOT  
#### 当前版本支持：  
1.目前仅支持查询功能  
2.当前支持语法  
  2.1.暂不支持函数模式。  
  2.2.select仅支持查询字段（暂不支持去重(distinct)，聚合(max,count,min,avg))，不支持select * 功能，暂不支持别名功能      
  2.3.暂不支持group by语法   
  2.4.where中暂不支持 in 语法以及嵌套查询(目前支持=,>=,<=,<,>,like)
  2.5.limit限制：count <= 500 且 min+count <= 5000 (OpenSearch限制)

#### 历史变更：  
1.初始化版本v0.0.1-SNAPSHOT，提供简单的SQL转换器支持