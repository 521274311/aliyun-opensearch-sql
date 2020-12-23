# aliyun-opensearch-sql
### 提供阿里云开放搜索SQL支持

#### SQL使用语法（同Mysql语法）：  
select  
&ensp;&ensp;&ensp;&ensp;[Distinct打散]  
&ensp;&ensp;&ensp;&ensp;[count,sum,max,min(聚合) + (attribute_field)]  
&ensp;&ensp;&ensp;&ensp;[field(字段)..]  
from [appName1],[appName2],..  
where  
&ensp;&ensp;&ensp;&ensp;([index_field[=|like]character] [..OR|AND])  
&ensp;&ensp;&ensp;&ensp;[..AND|OR]  
&ensp;&ensp;&ensp;&ensp;([index_field[=|>|>=|<|<=]number] [..OR|AND])  
group by [attribute_field]  
order by [field] [asc|desc]..  
limit min,count  
例如：  
select name, count(), max(distinct group_id)  
where app_name  
where id>=1 and id<=100000 and name like 'dragons%'  
group by name  
limit 0, 100  
(PS：建议OpenSearch默认展示字段配置全部展示字段)
#### 当前版本：v0.0.1-SNAPSHOT  
#### 当前版本支持：  
1.目前仅支持查询功能  
2.当前支持语法  
  2.1.仅支持4种聚合函数：count，min，max，sum  
  2.2.select仅支持查询字段，暂不支持别名功能（聚合函数不支持嵌套，使用聚合函数必须要分组，暂不支持having语法，参考：https://help.aliyun.com/document_detail/180049.html）  
  2.3.where中暂不支持 in 语法以及嵌套查询(目前支持=,>=,<=,<,>,like)  
  2.4.limit限制：count <= 500 且 min+count <= 5000 (OpenSearch限制)  
  2.5.不支持地理位置查询  
#### 历史变更：  
1.初始化版本v0.0.1-SNAPSHOT，提供简单的SQL转换器支持  