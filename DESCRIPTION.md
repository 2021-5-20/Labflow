# LabFlow 项目描述

## GitHub Repository Description

面向研究生和小型实验室的开源科研工作流系统：管理论文、PDF、实验、AI 论文卡片和 Markdown 周报。

## 简短介绍

LabFlow 是一个面向研究生和小型实验室的开源 Research OS。它把研究项目、论文/PDF 管理、实验记录、AI 论文卡片和可编辑周报整合到一个本地可运行、后续可扩展的 monorepo 中。

## 作品集介绍

LabFlow 解决的是小型科研团队常见的工作流割裂问题：论文散落在文件夹和 Zotero 中，实验结果记录在表格或笔记里，周报又需要手动整理。LabFlow 通过 Spring Boot 后端、Vue 3 前端、FastAPI AI 服务、PostgreSQL 和 MinIO，把论文、PDF、实验、AI 总结和周报生成连接成一个完整闭环。

## 较长介绍

LabFlow 是一个本地优先的科研工作流系统，适合研究生、小型实验室和个人研究项目使用。当前版本已经实现研究仪表盘、项目管理、论文管理、PDF 上传与识别、MinIO 文件存储、实验记录、Markdown 周报预览与保存、AI 论文卡片、AI job 异步执行和 Docker Compose 部署。项目采用 monorepo 结构，后端使用 Java 17 + Spring Boot，前端使用 Vue 3 + TypeScript，AI 服务使用 FastAPI，并坚持 PostgreSQL-only 的数据库路线，为后续 pgvector RAG 和论文知识库扩展预留空间。
