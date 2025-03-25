export default {
    data() {
        return {
            allDevices: []
        }
    },
    methods: {
        async fetchAllDevices() {
            try {
                const res = await this.$request.get('/device/all')
                if (res.code === '200') {
                    this.allDevices = res.data
                    console.log("[device.js] fetchAllDevices success")
                }
            } catch (error) {
                this.$message.error('获取设备列表失败')
            }
        }
    },
    created() {
        this.fetchAllDevices()
    }
}