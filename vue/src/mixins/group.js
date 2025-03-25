export default {
    data() {
        return {
            allGroups: [],
            groups: [],
        }
    },
    methods: {
        async fetchAllGroups() {
            try {
                const res = await this.$request.get('/group/all')
                if (res.code === '200') {
                    this.allGroups = res.data
                    this.groups = res.data
                    console.log("[group.js] fetchAllGroups success")
                }
            } catch (error) {
                this.$message.error('获取组列表失败')
            }
        }
    },
    created() {
        this.fetchAllGroups()
    }
}