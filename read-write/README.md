# <center> shardingphere 读写分离配置 <center
## 环境
1. springboot
2. shardingphere
3. mysql
4. spring data jpa
> 使用shardingphere之前需要先配置好mysql主从关系：[mysql主从环境搭建](https://note.youdao.com/s/DnvoXWZK)

## pom文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.5.4</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <artifactId>sharding</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>sharding</name>
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
## 配置文件
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
```
## 实现
### 创建数据库
`CREATE database database0;`
### 实体类
```java
@Entity
@Table(name = "user")
@Data
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String name;

}
```
### repo
```java
@Repository
public interface UserRepo extends JpaRepository<User, Long> {
}
```
### service
```java
@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserRepo userRepository;

    @Override
    public User addUser(User user) {

        // 强制路由主库
//        HintManager.getInstance().setMasterRouteOnly();
        return userRepository.save(user);
    }

    @Override
    public List<User> list() {
        return userRepository.findAll();
    }
}
```
### controller
```java
@RestController
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/users")
    public Object list() {
        return userService.list();
    }

    @PostMapping("/add")
    public Object add(String name, String city) {
        User user = new User();
        user.setCity(city);
        user.setName(name);
        return userService.addUser(user);
    }
}
```
## 测试
### 增加user
> POST http://localhost:8888/add?name=hehua03&city=beijing
> 输出：
> `2022-03-01 11:35:55.910  INFO 48926 --- [nio-8888-exec-1] ShardingSphere-SQL                       : Actual SQL: master-0 ::: insert into user  (city, name) VALUES (?, ?) ::: [beijing, hehua03]`

可以看到写入数据实在master库
### 获取userList
> GET http://localhost:8888/users
> 输出：
> `2022-03-01 11:37:26.999  INFO 48926 --- [nio-8888-exec-3] ShardingSphere-SQL                       : Actual SQL: master-0-slave-0 ::: select user0_.id as id1_0_, user0_.city as city2_0_, user0_.name as name3_0_ from user user0_`

结果：
```json
[
  {
    "id": 1,
    "city": "shanghai",
    "name": "hehua"
  },
  {
    "id": 2,
    "city": "beijing",
    "name": "hehua"
  },
  {
    "id": 3,
    "city": "beijing",
    "name": "bbb"
  },
  {
    "id": 4,
    "city": "beijing",
    "name": "hehua03"
  }
]
```