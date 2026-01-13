db.timeline.createIndex(
  {
    "recipients.userId": 1,
    type: 1,
    date: -1,
    created: -1
  },
  { name: "timeline_user_news_date_created" }
);