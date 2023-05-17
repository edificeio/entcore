import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ZimbraService } from './zimbra.service';
import { RecallMail } from './recallmail.model';

describe('ZimbraService', () => {
  let service: ZimbraService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ZimbraService]
    });
    service = TestBed.inject(ZimbraService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getRecalledMails', () => {
    it('should return an Observable of an array of RecallMail objects', () => {
      const mockResponse = { recallMails: [
        { recallMailId: 1, userName: 'user1', comment: 'comment1', message: null, status: 'status1', action: null },
        { recallMailId: 2, userName: 'user2', comment: 'comment2', message: null, status: 'status2', action: null }
      ]};
      const mockStructureId = 'structure1';

      service.getRecalledMails(mockStructureId).subscribe((result: RecallMail[]) => {
        expect(result.length).toBe(2);
        expect(result[0].recallMailId).toBe(1);
        expect(result[1].userName).toBe('user2');
      });

      const mockRequest = httpTestingController.expectOne(`/zimbra/recall/structure/${mockStructureId}/list`);
      expect(mockRequest.request.method).toBe('GET');
      mockRequest.flush(mockResponse);
    });
  });

  describe('deleteRecalledMail', () => {
    it('should make a DELETE request to the expected URL', () => {
      const mockId = 1;

      service.deleteRecalledMail(mockId).subscribe(() => {
      });

      const mockRequest = httpTestingController.expectOne(`/zimbra/recall/${mockId}/delete`);
      expect(mockRequest.request.method).toBe('DELETE');
      mockRequest.flush({});
    });
  });

  describe('acceptRecalls', () => {
    it('should make a PUT request with the expected body', () => {
      const mockMailIds = [1, 2, 3];

      service.acceptRecalls(mockMailIds).subscribe(() => {
      });

      const mockRequest = httpTestingController.expectOne(`/zimbra/recall/accept/multiple`);
      expect(mockRequest.request.method).toBe('PUT');
      expect(mockRequest.request.body).toEqual({ ids: mockMailIds });
      mockRequest.flush({});
    });
  });
});
