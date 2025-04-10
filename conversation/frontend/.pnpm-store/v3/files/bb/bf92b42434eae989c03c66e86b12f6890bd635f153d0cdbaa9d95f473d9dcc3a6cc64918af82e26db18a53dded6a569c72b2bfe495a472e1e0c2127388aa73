# Image Resizer

The `ImageResizer` class provides functionality to resize and compress image files. Below is an overview of its methods and usage.

### Methods

#### `changeDimension(height: number, maxHeight: number, width: number, maxWidth: number): { height: number; width: number }`

This private static method adjusts the height and width of an image to fit within the specified maximum dimensions while maintaining the aspect ratio.

#### `renameFileNameExtension(filename: string, newExtension: string): string`

This private static method changes the file extension of a given filename to the specified new extension.

#### `resizeImage(image: HTMLImageElement, fileName: string, maxWidth: number, maxHeight: number, compressFormat = "jpeg", quality = 80): Promise<File>`

This private static method resizes an image to the specified dimensions and compresses it to the specified quality. It returns a promise that resolves to a `File` object.

#### `resizeImageFile(file: File, maxWidth: number = 1440, maxHeight: number = 1440, quality: number = 80): Promise<File>`

This public static method resizes and compresses an image file. It returns a promise that resolves to the resized image file.

### Usage

To use the `ImageResizer` class, you can call the `resizeImageFile` method with the image file and desired dimensions:

```typescript
import ImageResizer from "./ImageResizer";

const fileInput = document.getElementById("fileInput") as HTMLInputElement;

fileInput.addEventListener("change", async (event) => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file) {
    try {
      const resizedFile = await ImageResizer.resizeImageFile(
        file,
        800,
        600,
        70,
      );
      console.log("Resized file:", resizedFile);
    } catch (error) {
      console.error("Error resizing image:", error);
    }
  }
});
```

### Notes

- The `resizeImageFile` method only supports JPEG format for compression.
- Ensure that the image file is loaded correctly before attempting to resize it.
- The quality parameter should be a value between 0 and 100.
