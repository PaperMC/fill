use fill

db.projects.insertOne({
  createdAt: ISODate(),
  updatedAt: ISODate(),
  name: "test-project",
  displayName: "Test Project"
})

db.families.insertOne({
  project: db.projects.findOne({ name: "test-project" })._id,
  name: "1.0",
  createdAt: ISODate(), updatedAt: ISODate(),
  java: {
    version: {
      minimum: 17
    },
    flags: {
      recommended: [
        "-Xmx2G",
        "-XX:+UseG1GC"
      ]
    }
  }
})

db.versions.insertOne({
  project: db.projects.findOne({ name: "test-project" })._id,
  family: db.families.findOne({ name: "1.0" })._id,
  name: "1.0.0",
  createdAt: ISODate(),
  updatedAt: ISODate(),
  support: {
    status: "SUPPORTED"
  }
})
