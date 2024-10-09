import { check } from "k6";
import {
  assertOk,
  getHeaders,
  uploadFile
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";
import http from "k6/http";

const dataRootPath = __ENV.DATA_ROOT_PATH || "./";
const rootUrl = __ENV.ROOT_URL;

export function createBlog(session, user, nbPosts) {
  const headers = getHeaders(session);
  const blogName = `Blog - Stress Test - ${user.email}`;
  headers["content-type"] = "application/json";
  const payload = JSON.stringify({
    title: blogName,
    description: `Le blog de ${user.login}`,
    thumbnail: "/blog/public/img/blog.png",
    "comment-type": "NONE",
    "publish-type": "RESTRAINT",
  });
  let res = http.post(`${rootUrl}/blog`, payload, { headers });
  assertOk(res, "create blog");
  const blogId = JSON.parse(res.body)["_id"];
  for (let i = 0; i < nbPosts; i++) {
    const postPayload = JSON.stringify({
      title: `Post ${String(i).padStart(5, "0")} de ${user.login}`,
      content: `Le contenu du super post ${String(i).padStart(
        5,
        "0"
      )} du blog de ${user.login}`,
    });
    res = http.post(`${rootUrl}/blog/post/${blogId}`, postPayload, { headers });
    //sleep(0.5)
    assertOk(res, "create post");
    const postId = JSON.parse(res.body)["_id"];
    if (postId) {
      res = http.get(
        `${rootUrl}/blog/post/list/all/${blogId}?postId=${postId}`,
        { headers }
      );
      assertOk(res, "get post");
      check(res, {
        "post was not found right after its creation": (r) => {
          const posts = JSON.parse(r.body);
          return posts.length && posts.length > 0;
        },
      });
    }
  }
  return blogId;
}

export function printBlog(blogId) {
  const title = "My Blog";
  const boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
  const description = "My Best Blog";
  const keyword = "CM2";
  const school = "My School";
  const minAge = 5, maxAge = 10;
  const printUrl = `${rootUrl}/blog/print/${blogId}`
  const application = 'Blog'
  const lang = "fr_FR"
  const activity = "bpr.activityType.homework";
  const subject = "bpr.subjectArea.worldDiscovery";
  const formData = `
${boundary}\r\nContent-Disposition: form-data; name="title"\r\n\r\n${title}\r\n
${boundary}\r\nContent-Disposition: form-data; name="cover"; filename="blob"\r\nContent-Type: image/jpeg\r\n\r\nÿØÿà\u0000\u0010JFIF\u0000\u00016·\u0000\u0000\u0000\u0000\r\n
${boundary}\r\nContent-Disposition: form-data; name="teacherAvatarName"\r\n\r\nteacherAvatar_b92e3d37-16b0-4ed9-b4c3-992091687132\r\n
${boundary}\r\nContent-Disposition: form-data; name="teacherAvatarType"\r\n\r\nimage/png\r\n
${boundary}\r\nContent-Disposition: form-data; name="language"\r\n\r\n${lang}\r\n
${boundary}\r\nContent-Disposition: form-data; name="activityType[]"\r\n\r\n${activity}\r\n
${boundary}\r\nContent-Disposition: form-data; name="subjectArea[]"\r\n\r\n${subject}\r\n
${boundary}\r\nContent-Disposition: form-data; name="age[]"\r\n\r\n${minAge}\r\n
${boundary}\r\nContent-Disposition: form-data; name="age[]"\r\n\r\n${maxAge}\r\n
${boundary}\r\nContent-Disposition: form-data; name="description"\r\n\r\n${description}\r\n
${boundary}\r\nContent-Disposition: form-data; name="keyWords[]"\r\n\r\n${keyword}\r\n
${boundary}\r\nContent-Disposition: form-data; name="licence"\r\n\r\nCC-BY\r\n
${boundary}\r\nContent-Disposition: form-data; name="pdfUri"\r\n\r\n${printUrl}\r\n
${boundary}\r\nContent-Disposition: form-data; name="application"\r\n\r\n${application}\r\n
${boundary}\r\nContent-Disposition: form-data; name="resourceId"\r\n\r\n${blogId}\r\n
${boundary}\r\nContent-Disposition: form-data; name="teacherSchool"\r\n\r\n${school}\r\n
${boundary}--\r\n`;
  const headers = {
    "Content-Type": `multipart/form-data; boundary=${boundary}`,
  };
  const res = http.post(`${rootUrl}/appregistry/library/resource`, formData, {
    headers: headers,
  });
  assertOk(res, "print blog");
}


const fileToUpload = open(`${dataRootPath}/file-sample_500kB.odt`, "b");
export function previewFile(session){
    const uploadedFile = uploadFile(fileToUpload, session)
    const res = http.get(`${rootUrl}/workspace/document/preview/${uploadedFile._id}`)
    assertOk(res, "preview odt");
}