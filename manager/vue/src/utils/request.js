import axios from 'axios'
import router from "@/router";

// 创建可一个新的axios对象
const request = axios.create({
    baseURL: process.env.VUE_APP_BASE_API || 'http://localhost:9090', // 修改为完整的后端服务器地址
    timeout: 30000
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';        // 设置请求头格式

    // 统一添加token到请求头
    let user = JSON.parse(localStorage.getItem("xm-user") || '{}');  // 获取缓存的用户信息
    const token = localStorage.getItem('xm-token') || user.token || '';

    if (token) {
        config.headers['token'] = token;
    }

    // 特殊处理 multipart/form-data 类型的请求（上传文件）
    if (config.method === 'post' &&
        config.headers['Content-Type'] &&
        config.headers['Content-Type'].includes('multipart/form-data')) {
        // 对于文件上传，上面已经设置了token
    }

    return config
}, error => {
    console.error('request error: ' + error) // for debug
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;

        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        if (res.code === '401') {
            router.push('/login')
        }
        return res;
    },
    error => {
        console.error('response error: ' + error) // for debug
        // 检查是否为404错误
        if (error.response && error.response.status === 404) {
            console.error('请求的API端点不存在:', error.config.url);
        }
        return Promise.reject(error)
    }
)

export default request
