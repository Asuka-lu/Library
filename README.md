**# 图书馆管理系统（library-system）



## 本地快捷预览项目

第一步：运行 sql 文件夹下的`springboot-vue.sql`，创建`springboot-vue`数据库

第二步：双击 run 文件夹下的`start.cmd`，弹出的dos窗口不要关闭

第三步：浏览器访问`localhost:9090`，测试账号需要自己插入到数据库中



## 主要技术

SpringBoot、Mybatis-Plus、MySQL、Vue3、ElementPlus等



## 主要功能

管理员模块：注册、登录、书籍管理、读者管理、借阅管理、借阅状态、修改个人信息、修改密码

读者模块：注册、登录、查询图书信息、借阅和归还图书、查看个人借阅记录、修改个人信息、修改密码



## 主要功能截图

### 登录

登录支持两种用户角色：管理员和读者

输入正确的账号、密码后，系统会自动识别管理员或者用户，并跳转到相应主页。



![](images/login.png)



### 展示板页面

![dashboard](images/dashboard.png)

### 管理员界面

#### 图书管理

- 图书表格列表

   ![book](images/book.png)

- 添加图书

   ![add_book](images/add_book.png)

- 编辑图书

   ![edit_book](images/edit_book.png)

- 删除图书

   ![delete_book](images/delete_book.png)

   



#### 读者管理

- 读者管理的增删查改类似图书管理，不再赘述

  ![](images/reader.png)

  

#### 借阅管理

- 借阅记录查询

  ![](images/lendrecord.png)

- 借阅记录编辑

  ![lendrecord_edit](images/lendrecord_edit.png)

  #### 借阅状态
  
  ![lendstatus](images/lendstatus.png)



### 读者界面

#### 读者信息

- 编辑个人信息

  ![](images/person_edit.png)

- 修改密码

  点击修改密码按钮，修改密码

  ![](images/person_password.png)



#### 图书查询

![](images/book_search.png)

#### 借阅图书

![](images/lendbook.png)

![](images/lendbook_2.png)

#### 归还图书

![](images/returnbook.png)

#### 借阅记录

![](images/book_information.png)

#### 借阅状态

![book_status2](images/book_status2.png)



## 代码结构

### 前端

```shell
library-ui
├─api			// api接口
├─assets		// 资源文件	
│  ├─icon	 	// 图标
│  ├─img	 	// 图片
│  └─styles	 	// 样式
├─components	// 自定义Vue组件
├─layout		// 页面布局
├─router		// Vue路由
├─utils			// 工具函数
└─views			// 页面
```

![](images/ui.png)



### 后端

maven项目结构

```shell
library-serve
├─java
│  └─com
│      └─admin
│          └─library
│              ├─common			// 通用类
│              │  ├─base		// 基础类
│              │  └─config		// 配置类
│              ├─controller		// 控制层
│              ├─domain			// 实体类
│              ├─mapper			// 持久层
│              └─service		// 业务层
└─resources	// maven资源配置
```

![](images/application.png)



## 数据库

**使用Navicat生成ER模型图**

![](images/sql.png)
**基于人脸识别的自助式图书管理系统
项目简介
随着信息技术的快速发展，图书馆管理方式逐步向智能化、自动化方向转型。传统的图书借阅与归还过程依赖人工操作，存在效率低、易出错的缺点。本项目设计并开发了一套基于人脸识别技术的自助式图书管理系统，旨在通过自动化技术提高图书管理效率并提升用户体验。

本系统通过人脸识别技术验证用户身份，结合自助式图书管理模式，实现用户从借阅到归还的全流程自动化操作，减轻管理负担，同时提升服务质量。

功能概述
核心功能
用户身份验证

基于人脸识别的用户登录功能，无需实体借书证。
支持用户注册、登录，确保身份信息的唯一性与安全性。
图书借阅与归还

用户扫描图书的条形码或二维码完成借阅操作。
系统自动记录借阅信息并支持通过人脸识别快速归还图书。
图书管理

系统管理员可管理图书信息，支持增删改查功能。
管理员可查看借阅记录并进行统计分析。
推荐与查询

根据用户借阅记录智能推荐图书。
支持图书实时查询，包括库存状态和位置信息。
技术栈
前端：Vue.js
后端：Node.js / Express或者Spring Boot
数据库：SQLite 或者 PostgreSQL 或者 MySQL
人脸识别：Face-api.js
系统开发要求
团队合作

每组由 3-5 人组成，共同负责系统分析、前后端开发、数据库设计及团队协作。
确保系统功能完整，实现良好的用户体验与稳定性。
交付内容

项目源码：包括前端代码、后端代码以及数据库文件。
项目文档：提供详细的需求设计文档、系统测试文档、使用手册。
演示文件：包含系统功能演示PPT。
提交方式

将所有文件打包为 组名.zip，并发送至指定邮箱（lanhy2000@163.com）。
项目进度
时间	内容
第 1-2 节	项目分析：明确系统目标与功能需求，完成原型设计，确定技术栈（Vue、Node.js/Express或者Spring Boot、SQLite或者 PostgreSQL 或者 MySQL、Face-api.js）。
第 3-14 节	系统开发：前后端开发、数据库设计、功能实现与测试，撰写文档，完成项目联调与优化。
第 15-16 节	项目展示：进行功能演示，提交项目文档与成果总结，完成经验交流。
使用说明
环境配置

确保安装 (Node.js或者Tomcat) 和 (SQLite或者 PostgreSQL 或者 MySQL)。
前端开发需安装 Vue.js 环境，推荐使用 Vue CLI 创建项目。
运行步骤

下载项目源码(以node.js为例子)，进入项目根目录。
安装依赖：npm install
启动后端服务：node server.js
启动前端服务：npm run serve
使用浏览器访问项目运行地址。
人脸识别配置

下载 Face-api.js 模型文件，并放置到指定路径。
确保摄像头权限已启用，用于人脸识别功能。
联系方式
如需更多帮助或反馈问题，请联系团队邮箱：lanhy2000@163.com
