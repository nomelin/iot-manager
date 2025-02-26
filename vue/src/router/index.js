import Vue from 'vue'
import VueRouter from 'vue-router'
import {getItemWithExpiry} from "@/App"

Vue.use(VueRouter)

// 解决导航栏或者底部导航tabBar中的vue-router在3.0版本以上频繁点击菜单报错的问题。
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
    return originalPush.call(this, location).catch(err => err)
}

const routes = [
    {
        path: '/front',
        name: '首页',
        component: () => import('../views/Front.vue'),
        redirect: '/front/view_platform',
        children: [
            {
                path: 'view_platform',
                name: 'viewPlatform',
                meta: {name: '监测平台'},
                component: () => import('../views/front/ViewPlatform.vue')
            },
            {
                path: 'upload',
                name: 'upload',
                meta: {name: '文件上传'},
                component: () => import('../views/front/Upload.vue')
            },
            {
                path: 'base_query',
                name: 'baseQuery',
                meta: {name: '基础查询'},
                component: () => import('../views/front/BaseQuery.vue')
            },
            {
                path: 'template_manage',
                name: 'templateManage',
                meta: {name: '模板管理'},
                component: () => import('../views/front/TemplateManage.vue')
            },
            {
                path: 'device_manage',
                name: 'deviceManage',
                meta: {name: '设备管理'},
                component: () => import('../views/front/DeviceManage.vue')
            },
            {
                path: 'group_manage',
                name: 'groupManage',
                meta: {name: '组管理'},
                component: () => import('../views/front/GroupManage.vue')
            },
            {
                path: 'third_party',
                name: 'thirdParty',
                meta: {name: '第三方接口'},
                component: () => import('../views/front/ThirdParty.vue')
            }
        ]

    },
    {path: '/login', name: 'Login', meta: {name: '登录'}, component: () => import('../views/Login.vue')},
    {path: '/register', name: 'Register', meta: {name: '注册'}, component: () => import('../views/Register.vue')},
    {path: '*', name: 'NotFound', meta: {name: '无法访问'}, component: () => import('../views/404.vue')},
]

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes
})

// 这段代码是一个路由守卫，在 Vue Router 中使用路由守卫可以实现在导航触发时进行一些特定操作，比如验证用户身份、权限验证等。
// 首先，通过 router.beforeEach 注册了一个全局前置守卫，该守卫会在路由切换之前执行。
// 在守卫函数中，通过 localStorage.getItem("user") 获取了本地存储中的用户信息，并将其解析为一个对象。
// 然后，判断要跳转的路由路径 to.path 是否为根路径 /，即判断用户是否访问了网站的根路径。
// 如果用户未登录，则将路由重定向到登录页面 /login。
// 如果用户访问的不是根路径 /，则直接调用 next() 方法，继续执行下一个导航钩子。
router.beforeEach((to, from, next) => {
    let user = getItemWithExpiry("user");
    console.log("get user", user)
    if (to.path === '/' || to.path === '') {
        if (user && !(Object.keys(user).length === 0)) {
            next('/front')
        } else {
            next('/login')
        }
    } else {
        next()
    }
})

export default router
