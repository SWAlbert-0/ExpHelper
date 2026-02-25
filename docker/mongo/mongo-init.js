db = db.getSiblingDB("expHlp");

if (!db.getUser("user")) {
  db.createUser({
    user: "user",
    pwd: "123456",
    roles: [{ role: "readWrite", db: "expHlp" }]
  });
}

const collections = ["probInstMgr", "algLibMgr", "exePlanMgr", "userMgr", "algRltSave"];
collections.forEach(function(name) {
  if (!db.getCollectionNames().includes(name)) {
    db.createCollection(name);
  }
});

const initAdminUser = process.env.APP_INIT_ADMIN_USER;
const initAdminPassword = process.env.APP_INIT_ADMIN_PASSWORD;
const initAdminEmail = process.env.APP_INIT_ADMIN_EMAIL || "";
const initAdminWechat = process.env.APP_INIT_ADMIN_WECHAT || "";

if (initAdminUser && initAdminPassword) {
  if (db.userMgr.countDocuments({ userName: initAdminUser }) === 0) {
    db.userMgr.insertOne({
      userName: initAdminUser,
      role: 1,
      password: initAdminPassword,
      email: initAdminEmail,
      wechat: initAdminWechat
    });
  }
} else {
  print("[WARN] APP_INIT_ADMIN_USER/APP_INIT_ADMIN_PASSWORD 未配置，已跳过默认管理员初始化。");
}
