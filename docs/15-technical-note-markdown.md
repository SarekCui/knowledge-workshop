# 技术 Note Markdown 设计

## 1. 定位

Note 保持学习笔记语义，公开后承担技术知识分享能力。正文 `content` 从普通文本明确为
Markdown 源文；历史纯文本无需迁移，仍可按普通段落渲染。

本批只调整前端编辑和展示，不修改 Note API、数据库字段、幂等键或发布状态机。

## 2. 编辑与展示

- 编辑器提供标题、粗体、行内代码、代码块、引用、列表和链接快捷操作。
- 编辑区与实时预览共享 `NoteContext`，不通过多层属性传递状态和操作方法。
- 公开详情支持 GFM 表格、任务列表、引用、链接、技术代码块和标题锚点。
- 至少存在两个 Markdown 标题时展示自动目录；重复标题生成稳定的递增锚点。
- 列表卡片只生成纯文本摘要，不执行完整 Markdown 高亮，避免列表渲染成本随文章数量增长。

## 3. 安全边界

- 不启用 Markdown 原始 HTML，渲染树经过 `rehype-sanitize` 白名单清洗。
- 外部链接使用 `target="_blank"`，并增加 `rel="noreferrer noopener"`。
- 代码高亮输入始终按文本处理，高亮 HTML 仅来自 highlight.js 转换结果。
- 首批按需注册 Java、JavaScript、TypeScript、SQL、Bash、YAML、JSON、XML、CSS、Python 和 Markdown，未知语言退化为纯代码块。

## 4. 已衔接能力

- 技术标签、标签筛选和标签搜索已在下一子批交付，见 `docs/16-technical-note-tags.md`。
- 正文站内图片上传、私有对象授权和异常清理已交付，见 `docs/17-technical-note-images.md`。

## 5. 后续范围

- 自动摘要、版本历史、举报审核和内容治理。

以上能力在实现和验收前不标记为已交付。
