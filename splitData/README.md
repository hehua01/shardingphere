# <center>shardingsphere分库分表<center>
> 继[shardingsphere读写分离](https://note.youdao.com/s/6IPU2Hcj) 后，现在来搭建shardingsphere分库分表
## 同一数据库下分至不同表
### 环境
同[shardingsphere读写分离](https://note.youdao.com/s/6IPU2Hcj)
### pom文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.6.4</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>splitData</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>splitData</name>
    <description>Demo project for Spring Boot</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.26</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>4.0.0-RC1</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>

```
### 配置文件
```yml
server: # 端口号
  port: 8888

spring:
  main:
    allow-bean-definition-overriding: true # 允许重名的bean可以被覆盖

  shardingsphere:
    dataSource:
      names: master-0, master-0-slave-0
      master-0:
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3307/database0?useUnicode=true&characterEncoding=utf8&tinyInt1isBit=false&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        #最大连接数
        maxPoolSize: 20
      master-0-slave-0: # 配置从库
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3308/database0?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        maxPoolSize: 20

    props:
      sql:
        show: true # 开启SQL显示，默认值: false，注意：仅配置读写分离时不会打印日志！！！
    sharding:
      master-slave-rules:
        ds_0:
          master-data-source-name: master-0
          slave-data-source-names: master-0-slave-0
          load-balance-algorithm-type: round_robin #主从规则轮询
      tables:
        course:
          # *** 注意此处 ***
          actual-data-nodes: ds_0.course_$->{1..2}
          key-generator:
            column: cid
            type: SNOWFLAKE
          table-strategy:
            inline:
              sharding-column: cid
              algorithm-expression: course_$->{cid % 2 + 1}
  jpa:
    generate-ddl: true
```
***注意***
上述配置中的`actual-data-nodes: ds_0.course_$->{1..2}`中的ds_0代表一组数据源，即`master-slave-rules: ds_0`的值
### 实体类
```java
package com.example.splitdata.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @Date 2022/3/1 15:50
 * @Description
 */
@Entity
@Table
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Course implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String cname;
    private long userId;
    private String status;
}

```

### repo
```java
package com.example.splitdata.repository;

import com.example.splitdata.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Date 2022/3/1 15:51
 * @Description
 */
@Repository
public interface CourseRepo extends JpaRepository<Course, Long> {
}

```
### service
```java
package com.example.splitdata.impl;

import com.example.splitdata.entity.Course;
import com.example.splitdata.repository.CourseRepo;
import com.example.splitdata.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Date 2022/3/1 15:54
 * @Description
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepo courseRepo;

    @Override
    public Course add(Course course) {
        return courseRepo.save(course);
    }

    @Override
    public List<Course> list() {
        return courseRepo.findAll();
    }
}

```
### controller
```java
package com.example.splitdata.controller;

import com.example.splitdata.entity.Course;
import com.example.splitdata.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Date 2022/3/1 15:55
 * @Description
 */
@RestController
public class CourseController {
    @Resource
    private CourseService courseService;

    @GetMapping("/courses")
    public Object list() {
        return courseService.list();
    }

    @PostMapping("/add")
    public Object add(String name, Long userId) {
        Course course = Course.builder().cname(name).userId(userId).status("NORMAL").build();
        return courseService.add(course);
    }
}

```
### 测试
#### 添加
第一次：

> POST http://localhost:8888/add?name=hehua&userId=4

结果：
`2022-03-01 16:49:02.727  INFO 62068 --- [nio-8888-exec-1] ShardingSphere-SQL                       : Actual SQL: master-0 ::: insert into course_2  (cname, status, user_id, cid) VALUES (?, ?, ?, ?) ::: [hehua, NORMAL, 4, 705460720849911809]`

第二次：

> POST http://localhost:8888/add?name=hehua&userId=5

结果：
`2022-03-01 16:50:01.004  INFO 62068 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-0 ::: insert into course_1  (cname, status, user_id, cid) VALUES (?, ?, ?, ?) ::: [hehua, NORMAL, 5, 705460965491081216]`

可以看到，两次结果均是写入了主服务器并且写在了不同库中（cid为偶数的插入到了course_1，奇数的插入到了course_2）
#### 获取列表
> GET http://localhost:8888/courses

结果：
`2022-03-01 17:26:26.730  INFO 63244 --- [nio-8888-exec-5] ShardingSphere-SQL                       : Actual SQL: master-0-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_1 course0_
2022-03-01 17:26:26.730  INFO 63244 --- [nio-8888-exec-5] ShardingSphere-SQL                       : Actual SQL: master-0-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_2 course0_
`

可以看到是从`course_1`和`course_2`中进行查询。

数据库实际结果：
```SQL
mysql> select * from course_1;
+--------------------+-------+---------+--------+
| cid                | cname | user_id | status |
+--------------------+-------+---------+--------+
| 705458764412616704 | hehua |       2 | NORMAL |
| 705460965491081216 | hehua |       5 | NORMAL |
+--------------------+-------+---------+--------+
2 rows in set (0.00 sec)

mysql> select * from course_2;
+--------------------+-------+---------+--------+
| cid                | cname | user_id | status |
+--------------------+-------+---------+--------+
| 705458639552380929 | hehua |       1 | NORMAL |
| 705460203562205185 | hehua |       3 | NORMAL |
| 705460720849911809 | hehua |       4 | NORMAL |
| 705469082903773185 | hehua |       6 | NORMAL |
+--------------------+-------+---------+--------+
4 rows in set (0.00 sec)
```
## 不用数据库下不同表
> 环境和pom文件以及各个类与 同一数据库下分至不同表保持 一致
### 配置文件
```yml
server: # 端口号
  port: 8888

spring:
  main:
    allow-bean-definition-overriding: true # 允许重名的bean可以被覆盖

  shardingsphere:
    dataSource:
      names: master-0, master-0-slave-0, master-1, master-1-slave-0
      master-0:
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3307/database0?useUnicode=true&characterEncoding=utf8&tinyInt1isBit=false&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        #最大连接数
        maxPoolSize: 20
      master-0-slave-0: # 配置从库
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3308/database0?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        maxPoolSize: 20

      master-1:
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3307/database1?useUnicode=true&characterEncoding=utf8&tinyInt1isBit=false&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        #最大连接数
        maxPoolSize: 20
      master-1-slave-0: # 配置从库
        type: com.zaxxer.hikari.HikariDataSource
        driverClassName: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://127.0.0.1:3308/database1?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
        username: root
        password: 123456
        maxPoolSize: 20

    props:
      sql:
        show: true # 开启SQL显示，默认值: false，注意：仅配置读写分离时不会打印日志！！！
    sharding:
      master-slave-rules:
        ds_0:
          master-data-source-name: master-0
          slave-data-source-names: master-0-slave-0
          load-balance-algorithm-type: round_robin #主从规则轮询
        ds_1:
          master-data-source-name: master-1
          slave-data-source-names: master-1-slave-0
          load-balance-algorithm-type: round_robin #主从规则轮询
      tables:
        course:
          actual-data-nodes: ds_$->{0..1}.course_$->{1..2}
          key-generator:
            column: cid
            type: SNOWFLAKE
          database-strategy:
            inline:
              sharding-column: user_id
              # use_id 为偶数，到ds_0，奇数到ds_1
              algorithm-expression: ds_$->{user_id % 2}
          table-strategy:
            inline:
              sharding-column: cid
              # cid 为偶数，到course_1，奇数到course_2
              algorithm-expression: course_$->{cid % 2 + 1}
  jpa:
    generate-ddl: true
```
### 测试
#### 添加
> POST http://localhost:8888/add?name=hehua&userId=7
> 
userId 为奇数，应该到ds_1

结果：
`2022-03-01 17:41:45.880  INFO 64043 --- [nio-8888-exec-1] ShardingSphere-SQL                       : Actual SQL: master-1 ::: insert into course_2  (cname, status, user_id, cid) VALUES (?, ?, ?, ?) ::: [hehua, NORMAL, 7, 705473988100358145]`
根据上述，应该在ds_1的course_2中能找到

mysql查询：
```SQL
mysql> use database1;
Database changed
mysql> select * from course_2;
+--------------------+-------+---------+--------+
| cid                | cname | user_id | status |
+--------------------+-------+---------+--------+
| 705473988100358145 | hehua |       7 | NORMAL |
+--------------------+-------+---------+--------+
1 row in set (0.00 sec)
```
#### 列表
> GET http://localhost:8888/courses

结果：

`2022-03-01 17:47:13.338  INFO 64043 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-0-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_1 course0_

2022-03-01 17:47:13.338  INFO 64043 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-0-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_2 course0_

2022-03-01 17:47:13.338  INFO 64043 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-1-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_1 course0_

2022-03-01 17:47:13.338  INFO 64043 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-1-slave-0 ::: select course0_.cid as cid1_0_, course0_.cname as cname2_0_, course0_.status as status3_0_, course0_.user_id as user_id4_0_ from course_2 course0_`

可以看到，都去了从库查询
