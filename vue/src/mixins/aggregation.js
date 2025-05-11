export default {
    data() {
        return {
            // 聚合相关数据
            aggregationTime: 0,
            queryAggregateFunc: null,
            aggregationTimeOptions: [
                {label: '不聚合', value: 0},
                {label: '1ms', value: 1},
                {label: '1s', value: 1000},
                {label: '5s', value: 5000},
                {label: '15s', value: 15000},
                {label: '30s', value: 30000},
                {label: '1m', value: 60000},
                {label: '5m', value: 300000},
                {label: '15m', value: 900000},
                {label: '30m', value: 1800000},
                {label: '1h', value: 3600000},
                {label: '2h', value: 7200000},
                {label: '6h', value: 21600000},
                {label: '12h', value: 43200000},
                {label: '1d', value: 86400000},
                {label: '3d', value: 259200000},
                {label: '1w', value: 604800000},
                {label: '2w', value: 1209600000},
            ],
            aggregateFuncOptions: [],
        }
    },
    methods: {
        async loadAggregateFunctions() {
            try {
                const res = await this.$request.get('/data/aggregateFuncs');
                if (res.code === '200') {
                    this.aggregateFuncOptions = res.data.map(item => ({
                        code: item.code,
                        name: item.name,
                        desc: item.desc,
                    }));
                    console.log("[aggregation.js] loadAggregateFunctions success");
                } else {
                    this.$message.error(res.msg);
                }
            } catch (error) {
                this.$message.error('获取聚合函数失败');
            }
        },
        handleAggregationTimeChange(value) {
            if (value === 0) {
                this.queryAggregateFunc = null;
            }
        },
    },
    created() {
        this.loadAggregateFunctions(); // 初始化加载聚合函数
    }
}