import axios from 'axios'
import router from "@/router";
import {getItemWithExpiry} from "@/App"

// 创建可一个新的axios对象
const request = axios.create({
    baseURL: process.env.VUE_APP_BASEURL,   // 后端的接口地址  ip:port
    timeout: 60000                          // 60s请求超时
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {
    //上传文件时，不设置Content-Type
    if (!config.url.includes('/files/upload')) {
        config.headers['Content-Type'] = 'application/json;charset=utf-8'; // 设置请求头格式
    }
    let user = getItemWithExpiry("user")  // 获取缓存的用户信息
    // console.log("user: "+user)
    if (user && Object.keys(user).length > 0 && user.token) {
        config.headers['token'] = user.token  // 设置请求头
    }
    return config
}, error => {
    console.error('请求错误: ' + error) // for debug
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
        if (res.code === '401' || res.code === '402' || res.code === '403' || res.code === '406' || res.code === '407'
            || res.code === '502' || res.code === '504') {
            router.push('/login')
        }
        return res;
    },
    error => {
        console.error('响应错误: ' + error) // for debug
        return Promise.reject(error)
    }
)


export default request