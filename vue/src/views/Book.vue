<template>
  <div class="home" style="padding: 10px">
    <!-- 搜索 -->
    <div style="margin: 10px 0;">
      <el-form inline="true" size="small">
        <el-form-item label="图书编号">
          <el-input v-model="search1" placeholder="请输入图书编号" clearable>
            <template #prefix><el-icon class="el-input__icon"><search /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="图书名称">
          <el-input v-model="search2" placeholder="请输入图书名称" clearable>
            <template #prefix><el-icon class="el-input__icon"><search /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="作者">
          <el-input v-model="search3" placeholder="请输入作者" clearable>
            <template #prefix><el-icon class="el-input__icon"><search /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="margin-left: 1%" @click="load" size="mini">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-button size="mini" type="danger" @click="clear">重置</el-button>
        </el-form-item>

        <el-form-item style="float: right" v-if="numOfOutDataBook != 0">
          <el-popconfirm
              confirm-button-text="查看"
              cancel-button-text="取消"
              icon-color="red"
              title="您有图书已逾期，请尽快归还"
              @confirm="toLook"
          >
            <template #reference>
              <el-button type="warning">逾期通知</el-button>
            </template>
          </el-popconfirm>
        </el-form-item>
      </el-form>
    </div>

    <!-- 顶部按钮区 -->
    <div style="margin: 10px 0;">
      <el-button type="primary" @click="add" v-if="user.role == 1">上架</el-button>

      <!-- ✅ 读者：只保留扫码借阅 -->
      <el-button type="primary" @click="openScanDialog" v-if="user.role == 2">扫码借阅</el-button>

      <el-popconfirm title="确认删除?" @confirm="deleteBatch" v-if="user.role == 1">
        <template #reference>
          <el-button type="danger" size="mini">批量删除</el-button>
        </template>
      </el-popconfirm>
    </div>

    <!-- 图书表格 -->
    <el-table
        :data="tableData"
        stripe
        border
        table-layout="fixed"
        @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="user.role == 1" type="selection" width="50" fixed="left" />

      <el-table-column prop="isbn" label="图书编号" width="150" sortable show-overflow-tooltip />
      <el-table-column prop="barcode" label="条形码" width="150" show-overflow-tooltip />
      <el-table-column prop="name" label="图书名称" min-width="180" show-overflow-tooltip />

      <!-- ✅ 价格列更窄 -->
      <el-table-column prop="price" label="价格" width="80" sortable align="center" />

      <el-table-column prop="author" label="作者" width="110" show-overflow-tooltip />
      <el-table-column prop="publisher" label="出版社" min-width="140" show-overflow-tooltip />

      <el-table-column prop="createTime" label="出版时间" width="120" sortable align="center" />

      <!-- ✅ 库存列窄一些 -->
      <el-table-column prop="stock" label="库存" width="80" sortable align="center" />

      <!-- 状态 -->
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template v-slot="scope">
          <el-tag v-if="Number(scope.row.stock) <= 0" type="warning">不可借</el-tag>
          <el-tag v-else type="success">可借</el-tag>
        </template>
      </el-table-column>

      <!-- ✅ 操作列加宽 + 按钮不挤 -->
      <el-table-column fixed="right" label="操作" width="170" align="center">
        <template v-slot="scope">
          <div class="op-row">
            <!-- 管理员 -->
            <el-button size="mini" @click="handleEdit(scope.row)" v-if="user.role == 1">修改</el-button>

            <el-popconfirm title="确认删除?" @confirm="handleDelete(scope.row.id)" v-if="user.role == 1">
              <template #reference>
                <el-button type="danger" size="mini">删除</el-button>
              </template>
            </el-popconfirm>

            <!-- 读者：仅还书 -->
            <el-popconfirm title="确认还书?" @confirm="returnBook(scope.row.isbn)" v-if="user.role == 2">
              <template #reference>
                <el-button
                    type="danger"
                    size="mini"
                    :disabled="isbnArray.indexOf(scope.row.isbn) === -1"
                >
                  还书
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 逾期详情 -->
    <el-dialog v-model="dialogVisible3" v-if="numOfOutDataBook != 0" title="逾期详情" width="50%">
      <el-table :data="outDateBook" style="width: 100%">
        <el-table-column prop="isbn" label="图书编号" />
        <el-table-column prop="bookName" label="书名" />
        <el-table-column prop="lendtime" label="借阅日期" />
        <el-table-column prop="deadtime" label="截至日期" />
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="dialogVisible3 = false">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分页 -->
    <div style="margin: 10px 0">
      <el-pagination
          v-model:currentPage="currentPage"
          :page-sizes="[5, 10, 20]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </div>

    <!-- ✅ 扫码借阅弹窗 -->
    <el-dialog v-model="scan.dialogVisible" title="扫码借阅" width="520px" @closed="stopScan">
      <div style="margin-bottom: 8px; font-size: 13px; color:#666;">
        请将条形码/二维码对准摄像头。识别成功后会自动借阅。
      </div>

      <div class="video-box">
        <video ref="scanVideo" autoplay muted playsinline class="video"></video>
      </div>

      <div style="margin-top: 10px; display:flex; gap: 10px;">
        <el-button type="primary" style="flex: 1" :loading="scan.loading" @click="startScan">
          开始扫描
        </el-button>
        <el-button style="flex: 1" @click="scan.dialogVisible=false">
          关闭
        </el-button>
      </div>

      <div style="margin-top: 10px;">
        <el-input v-model="scan.manualCode" placeholder="也可手动输入条形码/ISBN">
          <template #append>
            <el-button :loading="scan.loading" @click="borrowByCode(scan.manualCode)">借阅</el-button>
          </template>
        </el-input>
      </div>

      <div v-if="scan.status" style="margin-top: 8px; font-size: 12px; color:#999;">
        {{ scan.status }}
      </div>
    </el-dialog>

    <!-- 管理员上架 -->
    <el-dialog v-model="dialogVisible" title="上架书籍" width="30%">
      <el-form :model="form" label-width="120px">
        <el-form-item label="图书编号">
          <el-input style="width: 80%" v-model="form.isbn"></el-input>
        </el-form-item>

        <el-form-item label="条形码">
          <el-input style="width: 80%" v-model="form.barcode" placeholder="扫码枪/手动输入"></el-input>
        </el-form-item>

        <el-form-item label="库存数量">
          <el-input-number
              style="width: 80%"
              v-model="form.stock"
              :min="0"
              :step="1"
              controls-position="right"
          />
        </el-form-item>

        <el-form-item label="图书名称">
          <el-input style="width: 80%" v-model="form.name"></el-input>
        </el-form-item>

        <el-form-item label="价格">
          <el-input style="width: 80%" v-model="form.price"></el-input>
        </el-form-item>

        <el-form-item label="作者">
          <el-input style="width: 80%" v-model="form.author"></el-input>
        </el-form-item>

        <el-form-item label="出版社">
          <el-input style="width: 80%" v-model="form.publisher"></el-input>
        </el-form-item>

        <el-form-item label="出版时间">
          <el-date-picker
              value-format="YYYY-MM-DD"
              type="date"
              style="width: 80%"
              clearable
              v-model="form.createTime"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 管理员修改 -->
    <el-dialog v-model="dialogVisible2" title="修改书籍信息" width="30%">
      <el-form :model="form" label-width="120px">
        <el-form-item label="图书编号">
          <el-input style="width: 80%" v-model="form.isbn"></el-input>
        </el-form-item>

        <el-form-item label="条形码">
          <el-input style="width: 80%" v-model="form.barcode" placeholder="扫码枪/手动输入"></el-input>
        </el-form-item>

        <el-form-item label="库存数量">
          <el-input-number
              style="width: 80%"
              v-model="form.stock"
              :min="0"
              :step="1"
              controls-position="right"
          />
        </el-form-item>

        <el-form-item label="图书名称">
          <el-input style="width: 80%" v-model="form.name"></el-input>
        </el-form-item>

        <el-form-item label="价格">
          <el-input style="width: 80%" v-model="form.price"></el-input>
        </el-form-item>

        <el-form-item label="作者">
          <el-input style="width: 80%" v-model="form.author"></el-input>
        </el-form-item>

        <el-form-item label="出版社">
          <el-input style="width: 80%" v-model="form.publisher"></el-input>
        </el-form-item>

        <el-form-item label="出版时间">
          <el-date-picker
              value-format="YYYY-MM-DD"
              type="date"
              style="width: 80%"
              clearable
              v-model="form.createTime"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible2 = false">取 消</el-button>
          <el-button type="primary" @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from "../utils/request";
import { ElMessage } from "element-plus";
import { BrowserMultiFormatReader } from "@zxing/browser";

export default {
  name: "Book",
  created() {
    let userStr = sessionStorage.getItem("user") || "{}";
    this.user = JSON.parse(userStr);
    this.load();
  },
  data() {
    return {
      form: {},
      dialogVisible: false,
      dialogVisible2: false,

      search1: "",
      search2: "",
      search3: "",
      total: 0,
      currentPage: 1,
      pageSize: 10,
      tableData: [],
      user: {},

      ids: [],
      bookData: [],
      isbnArray: [],
      outDateBook: [],
      numOfOutDataBook: 0,
      dialogVisible3: false,

      // ✅ 扫码状态
      scan: {
        dialogVisible: false,
        loading: false,
        status: "",
        manualCode: "",
        reader: null,
        controls: null,
        handling: false,
      },
    };
  },
  methods: {
    handleSelectionChange(val) {
      this.ids = val.map((v) => v.id);
    },
    deleteBatch() {
      if (!this.ids.length) {
        ElMessage.warning("请选择数据！");
        return;
      }
      request.post("/book/deleteBatch", this.ids).then((res) => {
        if (res.code === "0") {
          ElMessage.success("批量删除成功");
          this.load();
        } else {
          ElMessage.error(res.msg);
        }
      });
    },
    load() {
      this.numOfOutDataBook = 0;
      this.outDateBook = [];
      this.isbnArray = [];

      request
          .get("/book", {
            params: {
              pageNum: this.currentPage,
              pageSize: this.pageSize,
              search1: this.search1,
              search2: this.search2,
              search3: this.search3,
            },
          })
          .then((res) => {
            this.tableData = (res.data && res.data.records) || [];
            this.total = (res.data && res.data.total) || 0;
          });

      // 读者：拉取自己借阅中书籍，用于“还书按钮是否可点 + 逾期提示”
      if (this.user.role == 2) {
        request
            .get("/bookwithuser", {
              params: {
                pageNum: "1",
                pageSize: 9999,
                search1: "",
                search2: "",
                search3: this.user.id,
              },
            })
            .then((res) => {
              this.bookData = (res.data && res.data.records) || [];

              const now = new Date();
              for (let i = 0; i < this.bookData.length; i++) {
                const it = this.bookData[i];
                this.isbnArray.push(it.isbn);

                const d = new Date(it.deadtime);
                if (it.deadtime && d < now) {
                  this.outDateBook.push({
                    isbn: it.isbn,
                    bookName: it.bookName,
                    deadtime: it.deadtime,
                    lendtime: it.lendtime,
                  });
                }
              }
              this.numOfOutDataBook = this.outDateBook.length;
            });
      }
    },
    clear() {
      this.search1 = "";
      this.search2 = "";
      this.search3 = "";
      this.load();
    },
    handleDelete(id) {
      request.delete("/book/" + id).then((res) => {
        if (res.code === "0") ElMessage.success("删除成功");
        else ElMessage.error(res.msg);
        this.load();
      });
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row));
      if (this.form.stock === undefined || this.form.stock === null) this.form.stock = 0;
      this.dialogVisible2 = true;
    },
    add() {
      this.dialogVisible = true;
      this.form = { stock: 1 };
    },
    save() {
      if (this.form.stock === undefined || this.form.stock === null) this.form.stock = 0;
      this.form.status = Number(this.form.stock) > 0 ? "1" : "0";

      if (!this.form.barcode || String(this.form.barcode).trim() === "") {
        ElMessage.error("条形码不能为空");
        return;
      }

      if (this.form.id) {
        request.put("/book", this.form).then((res) => {
          if (res.code === "0") ElMessage.success("修改成功");
          else ElMessage.error(res.msg);
          this.dialogVisible2 = false;
          this.load();
        });
      } else {
        this.form.borrownum = 0;
        request.post("/book", this.form).then((res) => {
          if (res.code === "0") ElMessage.success("上架成功");
          else ElMessage.error(res.msg);
          this.dialogVisible = false;
          this.load();
        });
      }
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize;
      this.load();
    },
    handleCurrentChange(pageNum) {
      this.currentPage = pageNum;
      this.load();
    },
    toLook() {
      this.dialogVisible3 = true;
    },

    // ✅ 打开扫码弹窗
    openScanDialog() {
      if (this.user.role != 2) return;

      if (this.bookData.length >= 5) {
        ElMessage.warning("最多只能借5本书");
        return;
      }
      if (this.numOfOutDataBook !== 0) {
        ElMessage.warning("存在逾期书籍，请先归还");
        return;
      }

      this.scan.dialogVisible = true;
      this.scan.status = "点击“开始扫描”后对准条形码/二维码";
      this.scan.manualCode = "";
      this.scan.handling = false;
    },

    // ✅ 开始扫码（识别到一次就借阅并停止）
    async startScan() {
      if (this.scan.loading) return;

      const video = this.$refs.scanVideo;
      if (!video) {
        ElMessage.error("视频组件未初始化");
        return;
      }

      try {
        this.scan.loading = true;
        this.scan.status = "正在打开摄像头...";

        if (!this.scan.reader) this.scan.reader = new BrowserMultiFormatReader();

        this.scan.controls = await this.scan.reader.decodeFromVideoDevice(
            undefined,
            video,
            async (result, err, controls) => {
              if (result && !this.scan.handling) {
                this.scan.handling = true;
                const text = result.getText();
                this.scan.status = `识别到：${text}，正在借阅...`;

                try {
                  await this.borrowByCode(text);
                  this.scan.dialogVisible = false;
                } finally {
                  if (controls) controls.stop();
                  this.scan.handling = false;
                }
              }
            }
        );

        this.scan.status = "摄像头已开启，请对准码...";
      } catch (e) {
        console.error(e);
        ElMessage.error("无法开启扫码：请检查浏览器摄像头权限（建议 localhost 或 https）");
      } finally {
        this.scan.loading = false;
      }
    },

    // ✅ 关闭扫码（停摄像头）
    stopScan() {
      try {
        if (this.scan.controls) this.scan.controls.stop();
        if (this.scan.reader) this.scan.reader.reset();
      } catch (e) {}
      this.scan.controls = null;
      this.scan.status = "";
      this.scan.loading = false;
      this.scan.handling = false;
    },

    // ✅ 借阅：只允许扫码/手动输入走这个
    async borrowByCode(code) {
      const c = (code || "").trim();
      if (!c) {
        ElMessage.warning("请先扫码或输入条形码/ISBN");
        return;
      }

      const res = await request.post("/borrow/scan", {
        userId: this.user.id,
        code: c,
      });

      if (res.code === "0") {
        ElMessage.success("借阅成功");
        this.load();
      } else {
        ElMessage.error(res.msg || "借阅失败");
      }
    },

    // ✅ 还书
    async returnBook(isbn) {
      const res = await request.post("/borrow/return", {
        userId: this.user.id,
        isbn,
      });
      if (res.code === "0") {
        ElMessage.success("还书成功");
        this.load();
      } else {
        ElMessage.error(res.msg || "还书失败");
      }
    },
  },
};
</script>

<style scoped>
.video-box {
  width: 100%;
  height: 260px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}
.video {
  width: 100%;
  height: 260px;
  object-fit: cover;
}

.op-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.op-row :deep(.el-button) {
  border-radius: 10px;
}
</style>
