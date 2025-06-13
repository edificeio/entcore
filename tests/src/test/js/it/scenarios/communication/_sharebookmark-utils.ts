import { getHeaders, ShareBookMark } from "../../../node_modules/edifice-k6-commons/dist/index.js";
import { check } from "k6";
import http, { RefinedResponse } from "k6/http";



export function checkNbBookmarkVisible(
  bookmark: ShareBookMark,
  expectedCount: number,
  checkName: string
) {
  const checks = {};
  checks[`${checkName} - Bookmark number`] = (b) => b.users.length === expectedCount;
  const ok = check(bookmark, checks);
  if (!ok) {
    console.error(checkName, bookmark.users.length);
  }
}