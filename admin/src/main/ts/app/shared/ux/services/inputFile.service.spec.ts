import { InputFileService } from './inputFile.service'
import { TestBed } from "@angular/core/testing";

describe('InputFileService', () => {
    let inputFileService: InputFileService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [InputFileService]
        });
        inputFileService = TestBed.get(InputFileService);
    });

    describe('validateFiles', () => {
        it('should return [file] when given FileList file1.jpg, maxFilesNumber 1 and allowedExtensions jpg)', () => {
            const blob = new Blob([""], { type: "text/html" });
            blob["name"] = "file1.jpg";
            const file = <File>blob;
            const fileList = {
              0: file,
              length: 1,
              item: (index: number) => file
            };

            let resFiles: File[];
            inputFileService
                .validateFiles(fileList, 1, ['jpg'])
                .subscribe(files => resFiles = files);
            expect(resFiles).toEqual([file]);
        });

        it('should return [file1, file2, file3] when given FileList file1.bmp, file2.jpg, file3.png, maxFilesNumber 3 and allowedExtensions jpg, png, bmp)', () => {
            const blob1 = new Blob([""], { type: "text/html" });
            blob1["name"] = "file1.bmp";
            const file1 = <File>blob1;

            const blob2 = new Blob([""], { type: "text/html" });
            blob2["name"] = "file2.jpg";
            const file2 = <File>blob2;

            const blob3 = new Blob([""], { type: "text/html" });
            blob3["name"] = "file3.png";
            const file3 = <File>blob3;

            const fileList = {
              0: file1,
              1: file2,
              2: file3,
              length: 3,
              item: (index: number) => file1
            };

            let resFiles: File[];
            inputFileService
                .validateFiles(fileList, 3, ['jpg', 'png', 'bmp'])
                .subscribe(files => resFiles = files);
            expect(resFiles).toEqual([file1, file2, file3]);
        });

        it('should throw "Only 1 file(s) allowed" error when given FileList file1.bmp, file2.jpg, maxFilesNumber 1 and allowedExtensions bmp, jpg, png)', () => {
            const blob1 = new Blob([""], { type: "text/html" });
            blob1["name"] = "file1.bmp";
            const file1 = <File>blob1;

            const blob2 = new Blob([""], { type: "text/html" });
            blob2["name"] = "file2.jpg";
            const file2 = <File>blob2;

            const fileList = {
              0: file1,
              1: file2,
              length: 2,
              item: (index: number) => file1
            };

            const maxFilesNumber = 1;

            let resError: string;
            inputFileService
                .validateFiles(fileList, maxFilesNumber, ['jpg', 'png', 'bmp'])
                .subscribe(() => {}, error => resError = error);
            expect(resError).toBe(`Only ${maxFilesNumber} file(s) allowed`);
        });

        it('should throw "Extension not allowed. Allowed extensions: [jpg]" error when given FileList file1.bmp, maxFilesNumber 1 and allowedExtensions jpg)', () => {
            const blob1 = new Blob([""], { type: "text/html" });
            blob1["name"] = "file1.bmp";
            const file1 = <File>blob1;

            const fileList = {
              0: file1,
              length: 1,
              item: (index: number) => file1
            };

            const allowedExtensions = ['jpg'];

            let resError: string;
            inputFileService
                .validateFiles(fileList, 1, allowedExtensions)
                .subscribe(() => {}, error => resError = error);
            expect(resError).toBe(`Extension not allowed. Allowed extensions: ${allowedExtensions}`)
        });
    });

    describe('isSrcExternalUrl', () => {
        it('should return true with src equals to http://image.com/image1', () => {
            expect(inputFileService.isSrcExternalUrl('http://image.com/image1')).toBe(true);
        });
        
        it('should return true with src equals to https://securedimage.com/image1', () => {
            expect(inputFileService.isSrcExternalUrl('https://securedimage.com/image1')).toBe(true);
        });

        it('should return false with src equals to /workspace/image1', () => {
            expect(inputFileService.isSrcExternalUrl('/workspace/image1')).toBe(false);
        });
    
        it('should return false with src equals to admin-large', () => {
            expect(inputFileService.isSrcExternalUrl('admin-large')).toBe(false);
        });
    });

    describe('isSrcWorkspace', () => {
        it('should return false with src equals to http://image.com/image1', () => {
            expect(inputFileService.isSrcWorkspace('http://image.com/image1')).toBe(false);
        });
        
        it('should return false with src equals to https://securedimage.com/image1', () => {
            expect(inputFileService.isSrcWorkspace('https://securedimage.com/image1')).toBe(false);
        });

        it('should return true with src equals to /workspace/image1', () => {
            expect(inputFileService.isSrcWorkspace('/workspace/image1')).toBe(true);
        });
    
        it('should return false with src equals to admin-large', () => {
            expect(inputFileService.isSrcWorkspace('admin-large')).toBe(false);
        });
    });
});
