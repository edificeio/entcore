import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { SearchTypeValue } from "src/app/core/enum/SearchTypeEnum";
import { UserModel } from "src/app/core/store/models/user.model";

@Injectable()
export class AdmcSearchService {
    constructor(private http: HttpClient) {
    }

    public async search(searchTerm: Array<string>, searchType: SearchTypeValue): Promise<Array<UserModel>> {
        if (!searchTerm) return [];

        let request = (searchType==='displayName' && searchTerm.length>=2)
            ? `/directory/user/admin/list?firstName=${searchTerm[0]}&lastName=${searchTerm[1]}&searchType=${searchType}`
            : `/directory/user/admin/list?searchTerm=${searchTerm[0]}&searchType=${searchType}`;
        
        const res = await this.http.get<Array<UserModel>>(request).toPromise();
        return res;
    }
}
