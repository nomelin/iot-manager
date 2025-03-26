export default {
    data() {
        return {
            createVisible: false,
            newDevice: {
                name: '',
                tags: [],
                config: {}
            },
            inputTag: '',
            selectedTemplateId: null,
            storageModes: [],
            deviceTypes: []
        }
    },
    computed: {
        selectedTemplate() {
            return this.templates?.find(t => t.id === this.selectedTemplateId)
        }
    },
    methods: {
        showCreateDialog(templateId = null) {
            this.createVisible = true
            this.newDevice = {name: '', tags: [], config: {}}
            this.selectedTemplateId = templateId
            this.inputTag = ''
        },
        handleNewTagAdd() {
            if (this.inputTag && !this.newDevice.tags.includes(this.inputTag)) {
                this.newDevice.tags.push(this.inputTag)
                this.inputTag = ''
            }
        },
        handleNewTagClose(index) {
            this.newDevice.tags.splice(index, 1)
        },
        async loadStorageModes() {
            if (this.storageModes?.length) return
            const res = await this.$request.get('/data/storageModes')
            if (res.code === '200') this.storageModes = res.data
        },
        async loadDeviceTypes() {
            if (this.deviceTypes?.length) return
            const res = await this.$request.get('/data/deviceTypes')
            if (res.code === '200') this.deviceTypes = res.data
        },
        validateCreate() {
            this.$refs.createForm.validate(valid => {
                if (valid && this.selectedTemplateId) this.createDevice()
                else this.$message.error('请选择模板')
            })
        },
        async createDevice() {
            console.log("newDevice", JSON.stringify(this.newDevice))
            console.log("selectedTemplateId", this.selectedTemplateId)
            try {
                const res = await this.$request.post('/device/add', {
                    device: this.newDevice,
                    templateId: this.selectedTemplateId
                })
                if (res.code === '200') {
                    this.createVisible = false
                    this.$message.success('创建设备成功')
                    if (typeof this.fetchDevices === 'function') await this.fetchDevices()
                } else this.$message.error(res.msg)
            } catch (error) {
                this.$message.error('创建设备失败')
            }
        }
    },
    created() {
        this.loadStorageModes()
        this.loadDeviceTypes()
    }
}