# aliyun-opensearch-sql
### 提供阿里云开放搜索SQL支持
##### 官方文档地址：https://help.aliyun.com/product/29102.html

##### 使用建议: 
##### 1.需要查询的字段至少创建一个与字段名同名索引
##### 2.需要查询的字段若支持设置为属性字段则添加到属性字段

#### SQL使用语法（同Mysql语法）：  
```
select  
        [Distinct + attribute_field_name(属性字段)]  
        [count,sum,max,min(聚合) + attribute_field_name(属性字段)]  
        [field_name(表字段)..]  
from [appName1],[appName2],..  
where  
        ([index_field_name(索引字段)[=|like]character] [..OR|AND])  
        [..AND|OR]  
        ([index_field_name(索引字段)[=|>|>=|<|<=|in|between]number|literal] [..OR|AND])  
group by [attribute_field_name(属性字段)]  
order by [field_name(表字段)] [asc|desc]..  
limit min,count
```
### 示例数据源
AppName: app_name  
TableName: person
  
**字段**：  
1. unique_id  int 主键  
2. uid int  
3. name short_text  
4. age int  

**索引**：  
1. default => name 中文-电商分析方式
2. id => unique_id 关键字分析方式
3. uid => uid 关键字分析方式  
4. name => name 模糊分析方式  
5. kname => name 中文-通用分析方式  
6. age => age 关键字分析方式  
7. id_range => unique_id Range分析方式

**属性**：  
unique_id, uid, age  

**默认展示字段**：  
unique_id, uid, name, age  

#### 普通字段查询示例 (like字段仅可用于模糊分析方式的索引中)
```
select id, uid, name, age
from app_name
where uid >= 1 and uid <= 1000 and name like 'dragons%' and age = 18
limit 10

内部解析结果:
query=name:'^dragons'
filter=uid>=1 AND uid<=1000 AND age=18
config.start=0
config.hit=10
```

(**PS: ~~想要触发query查询使用name like '%xxx%' 或 name="xxx" 均可~~
v0.0.3-SNAPSHOT版本仅支持使用 name like '%xxx%查询'**)

#### 普通字段去重查询示例
```
select distinct uid
from app_name
where uid >= 1 and uid <= 1000 and name like 'dragons%' and age = 18
```
  

#### 聚合查询示例：
```  
select name, count(), max(distinct uid)  
from app_name  
where uid>=1 and uid<=100000 and name like 'dragons%'  
group by name  
limit 0, 100
```  
(PS：建议OpenSearch默认展示字段配置全部展示字段)  

#### 获取SearchResult对象使用示例（默认Iterator.next()迭代一次搜索）：
```  
OpenSearch openSearch = new OpenSearch(ACCESS_KEY, SECRET, HOST);  
OpenSearchClient openSearchClient = new OpenSearchClient(openSearch);  
SearcherClient searcherClient = new SearcherClient(openSearchClient);  
SearcherClientQueryIterator iterator = new DefaultSearcherClientQueryIterator(searcherClient, SQL);  
while (iterator.hasNext()) {  
    System.out.println(iterator.next());  // SearchResult对象
}  
```
#### 获取单个元素对象使用示例（使用SearcherClientQueryIterator.hasNextOne() 与 SearcherClientQueryIterator.nextOne()获取单个元素对象）
```
OpenSearch openSearch = new OpenSearch(ACCESS_KEY, SECRET, HOST);  
OpenSearchClient openSearchClient = new OpenSearchClient(openSearch);  
SearcherClient searcherClient = new SearcherClient(openSearchClient);  
SearcherClientQueryIterator iterator = new DefaultSearcherClientQueryIterator(searcherClient, SQL);  
while (iterator.hasNextOne()) {  
    System.out.println(iterator.nextOne());  // com.aliyun.opensearch.sdk.dependencies.org.json.JSONObject对象
}  
```
#### 实战(常见问题汇总)
```sql
1.like 语法比价的值若不是OpenSearch模糊搜索索引左右需均带上%,即使是关键词索引需要精确匹配两边也需带上%。OpenSearch模糊查询文档：https://help.aliyun.com/document_detail/179439.html
select *
from app_name
where kname like '%dragons%' 
limit 10;
内部解析结果:
query=kname:'dragons'
若不加%,即where id like 'dragons'将会解析为
query=kname:'^dragons$'
索引若不支持模糊查询将无法查询得到结果

2.不要在like与其他表达式中混入OR连接符,OpenSearch查询是同时传入query与filter，若混用则可能导致结果不符合预期,示例如下:
select *
from app_name
where kname like '%dragons%' or unique_id=1
limit 10;
内部解析结果：
query=kname:'dragons'
filter=unique_id=1
不符合查询预期，应使用like统一转为query查询,改写如下：
select *
from app_name
where kname like '%dragons%' or id like '%1%'
limit 10;
这里id是unique_id的关键词索引，所以即使左右均加入%也是精确搜索,内部解析结果:
query=kname:'dragons' OR id:'1'
```

#### 当前版本: v0.0.3-SNAPSHOT
#### 当前版本支持：
1. 在v0.0.2-SNAPSHOT版本上,剔除了动态选择query与filter功能(防止判错导致查询失败), 转而使用强类型声明方式:  
```
1. 强制要求使用 like 方式声明query查询
2. 使用=, !=或<>, >=, <= 查询时内部均通过filter查询
 ```

2. 新增支持in语法(in语法也是通过filter查询)、between语法(between语法内部使用filter range功能, 仅支持数值类型与时间类型)、函数功能。具体参考: <a href="https://help.aliyun.com/document_detail/204346.html">OpenSearch Filter字句</a>。使用示例如下：
```
select *
from app_name
where id_range between 1 and 5 and age in (18, 19, 20);

插件内部将转换为: filter=id_range:[1,5] AND in(age, "18|19|20")
```
3. 新增default like查询支持, 示例如下:
```sql
select *
from app_name
where default like '%咸鱼%';
```
4. 转换器内部结构优化

<font style="font-weight: 700" color="red">PS: 新版本使用"="需要注意名称是否为属性名称(若不是则会导致查询失败, 旧版本使用"=" + 动态探测类型可能会注入query中, 新版本将"="改为"like"注入进query中, 旧版本使用"="注入进filter中则不用处理)
</font>

#### 历史变更：v0.0.2-SNAPSHOT
1. 仅支持查询功能  
2. 支持语法  
  2.1. 仅支持4种聚合函数：**count**，**min**，**max**，**sum**  
  2.2. **select** 仅支持查询字段，暂不支持别名功能（聚合函数不支持嵌套，使用聚合函数必须要分组，暂不支持having语法，参考：<a href="https://help.aliyun.com/document_detail/180049.html">aggregate字句</a>。  
  2.3. **where** 中暂不支持 **in** 语法以及嵌套查询（目前支持 **=**, **>=**, **<=**, **<**, **>**, **like**, **!=**, **<>**），PS：比较运算符暂时仅通过 **filter** 实现，**range** 实现正在开发中。  
  2.4. 默认使用 **scroll** 方式召回（ **scroll** 不支持 **distinct**, **aggregate** 语法; **sort** 语法仅支持 **int** 类型字段排序），若明确知道召回数小于等于 **5000**，则建议使用 **limit** 来触发 **hit** 方式召回。  
  2.5. 暂不支持地理位置查询  
#### 历史变更：  
1. 初始化版本**v0.0.1-SNAPSHOT**，提供默认的 **OpenSearch** 搜索迭代器  
2. 优化"="比较表达式在数值比较时使用filter处理
3. 修复Aggregate多分组时无法解析问题


### 优化记录
##### 2021-08-05
1. 要求显示声明query与filter, query通过"like"方式, 其他表达式均为filter, 示例如下:
```sql
filter示例-内部使用语法：filter=unique_id=1
select *
from app_name
where unique_id=1

query示例-内部使用：query=id:'1'
select *
from app_name
where id like '%1%'
(PS:这里id_kw为关键字索引, 该索引类型不支持模糊查询,因此带上%也为精确查询, 
为了支持OpenSearch模糊查询语法, 即^前缀起始符与$后缀终止符,普通查询若不带上%则会在生成的语句前后带分别带上^与$符
,后续将通过获取表信息与字段信息优化)
```
2. 新增in、between、filter内置函数功能(均通过filter查询)。
##### 2021-05-28
1. 增强filter中中文不等于支持，使用示例如下：
```sql
selec * from APP_NAME where title!='咸鱼' 
```
(PS: 使用!=与<>均可表示不等于)
其中要求title字段在Opensearch后台中添加为属性字段(目前字符类型支持属性字段的类型有: LITERAL)
