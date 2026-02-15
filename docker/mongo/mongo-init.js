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

if (db.userMgr.countDocuments({ userName: "admin" }) === 0) {
  db.userMgr.insertOne({
    userName: "admin",
    role: 1,
    password: "123456",
    email: "admin@example.com",
    wechat: "admin_wechat"
  });
}
