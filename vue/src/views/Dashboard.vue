<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in cards" :key="item.title">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">{{ item.title }}</div>
          </template>
          <div class="text item">
            <svg class="icon" aria-hidden="true">
              <use :xlink:href="item.icon" style="width: 100px"></use>
            </svg>
            <span class="text">{{ item.data }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div id="myTimer" style="margin-left: 15px; font-weight: 550;"></div>

    <!-- ✅ 热门推荐模块（按 borrownum 降序） -->
    <el-card class="rec-card" style="margin: 12px 10px 0 10px;">
      <template #header>
        <div class="rec-header">
          <span>热门推荐</span>
          <el-button size="small" @click="loadHotBooks" :loading="hot.loading">刷新</el-button>
        </div>
      </template>

      <el-skeleton v-if="hot.loading" :rows="6" animated />

      <el-empty v-else-if="hot.list.length === 0" description="暂无推荐数据" />

      <el-table
          v-else
          :data="hot.list"
          border
          stripe
          size="small"
          style="width: 100%"
      >
        <el-table-column label="#" type="index" width="55" />
        <el-table-column prop="name" label="书名" min-width="160" />
        <el-table-column prop="author" label="作者" min-width="120" />
        <el-table-column prop="publisher" label="出版社" min-width="140" />
        <el-table-column prop="isbn" label="ISBN" min-width="140" />
        <el-table-column prop="borrownum" label="借阅次数" width="90" />
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag v-if="Number(scope.row.stock) > 0" type="success">可借</el-tag>
            <el-tag v-else type="warning">不可借</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 8px; color:#999; font-size:12px;">
        说明：按全站图书借阅次数从高到低推荐前 {{ hot.limit }} 本。
      </div>
    </el-card>
  </div>
</template>

<script>
import request from "../utils/request";
import { ElMessage } from "element-plus";

export default {
  data() {
    return {
      cards: [
        { title: "已借阅", data: 0, icon: "#iconlend-record-pro" },
        { title: "总访问", data: 0, icon: "#iconvisit" },
        { title: "图书数", data: 0, icon: "#iconbook-pro" },
        { title: "用户数", data: 0, icon: "#iconpopulation" },
      ],
      hot: {
        loading: false,
        list: [],
        limit: 8, // 显示前几本
      },
    };
  },
  mounted() {
    this.circleTimer();
    this.loadDashboard();
    this.loadHotBooks();
  },
  methods: {
    circleTimer() {
      this.getTimer();
      setInterval(() => this.getTimer(), 1000);
    },
    getTimer() {
      const d = new Date();
      const el = document.getElementById("myTimer");
      if (el) el.innerHTML = d.toLocaleString();
    },

    loadDashboard() {
      request.get("/dashboard").then((res) => {
        if (res.code === 0 || res.code === "0") {
          this.cards[0].data = res.data.lendRecordCount;
          this.cards[1].data = res.data.visitCount;
          this.cards[2].data = res.data.bookCount;
          this.cards[3].data = res.data.userCount;
        } else {
          ElMessage.error(res.msg || "获取统计数据失败");
        }
      });
    },

    async loadHotBooks() {
      this.hot.loading = true;
      try {
        const res = await request.get("/book", {
          params: {
            pageNum: 1,
            pageSize: 9999,
            search1: "",
            search2: "",
            search3: "",
          },
        });

        if (!(res.code === "0" || res.code === 0)) {
          ElMessage.error(res.msg || "获取图书列表失败");
          this.hot.list = [];
          return;
        }

        const records = (res.data && res.data.records) ? res.data.records : [];
        this.hot.list = records
            .slice()
            .sort((a, b) => Number(b.borrownum || 0) - Number(a.borrownum || 0))
            .slice(0, this.hot.limit);
      } catch (e) {
        console.error(e);
        ElMessage.error("加载热门推荐异常");
      } finally {
        this.hot.loading = false;
      }
    },
  },
};
</script>

<style scoped>
.box-card {
  width: 80%;
  margin-bottom: 25px;
  margin-left: 10px;
}
.clearfix {
  text-align: center;
  font-size: 15px;
}
.text {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  vertical-align: super;
}
.icon {
  width: 50px;
  height: 50px;
  padding-top: 5px;
  padding-right: 10px;
}
.rec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
