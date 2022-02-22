import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { UserModel } from "src/app/core/store/models/user.model";

@Injectable()
export class AdmcSearchService {
    constructor(private http: HttpClient) {
    }

    public async search(searchTerm: string, searchType: string): Promise<Array<UserModel>> {
        if (!searchTerm) return [];
        
        const res = await this.http.
            get<Array<UserModel>>(`/directory/user/admin/list?searchTerm=${searchTerm}&searchType=${searchType}`).
            toPromise();

        return res;
    }
}
