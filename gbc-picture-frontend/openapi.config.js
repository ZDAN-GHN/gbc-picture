import { generateService } from '@umijs/openapi'

generateService({
  requestLibPath: "import request from '@/request'", /* 本项目用于发送请求的实例的导包语句 */
  schemaPath: 'http://localhost:8123/api/v2/api-docs', /* swagger文档的地址 */
  serversPath: './src', /* 生成代码保存路径 */
})
