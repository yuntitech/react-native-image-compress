#import "YTImageCompress.h"

@implementation YTImageCompress

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(compressImage:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  NSString *imageFileUri = [params valueForKey:@"imageFileUri"];
  NSNumber *maxWidthOrHeightNumber = [params valueForKey:@"maxWidthOrHeight"];
  NSNumber *maxFileSizeNumber = [params valueForKey:@"maxFileSize"];
  if (imageFileUri == nil || [imageFileUri isKindOfClass:NSString.class] == NO
      || maxWidthOrHeightNumber == nil || [maxWidthOrHeightNumber isKindOfClass:NSNumber.class] == NO
      || maxFileSizeNumber == nil || [maxFileSizeNumber isKindOfClass:NSNumber.class] == NO) {
    reject(@"parametersInvalid", @"check imageFileUri, maxWidthOrHeight, maxFileSize" ,nil);
    return;
  }
  
  UIImage *image = [UIImage imageWithContentsOfFile:imageFileUri];
  if (image == nil) {
    reject(@"imageInvalid", @"读取图片失败", nil);
    return;
  }
  
  // 高宽限制
  CGFloat maxWidthOrHeight = maxWidthOrHeightNumber.doubleValue;
  UIImage *scaledImage = [self scaleImage:image maxWidthOrHeight:maxWidthOrHeight];
  
  // 线性压缩
  NSInteger maxFileSize = maxFileSizeNumber.integerValue;
  CGFloat firstLevelQuality = [self findFirstLevelQualityWithImage:scaledImage maxFileSize:maxFileSize];
  
  // 二分法压缩，保证获取到最佳质量
  NSData *compressedData = [self binaryCompress:scaledImage firstLevelQuality:firstLevelQuality maxFileSize:maxFileSize];
  
  NSTimeInterval now = [NSDate timeIntervalSinceReferenceDate];
  NSString *fileName = [NSString stringWithFormat:@"YTImageCompress-compressed-image-%lf.jpg", now];
  NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:fileName];
  BOOL success = [compressedData writeToFile:filePath atomically:YES];
  if (success) {
    resolve(@{ @"compressedUri": filePath });
  } else {
    reject(@"writingImageToFileFailed", @"图片写入失败", nil);
  }
}

- (UIImage *)scaleImage:(UIImage *)image maxWidthOrHeight:(CGFloat)maxWidthOrHeight {
  CGFloat originalImageWidth = image.size.width;
  CGFloat originalImageHeight = image.size.height;
  BOOL isSizeTooLarge = originalImageWidth > maxWidthOrHeight || originalImageHeight > maxWidthOrHeight;
  if (isSizeTooLarge) {
    CGFloat maxSide = MAX(originalImageWidth, originalImageHeight);
    CGFloat scale = maxWidthOrHeight / maxSide;
    CGFloat scaledWidth = originalImageWidth * scale;
    CGFloat scaledHeight = originalImageHeight * scale;
    CGRect rect = CGRectMake(0.0, 0.0, scaledWidth, scaledHeight);
    UIGraphicsBeginImageContext(rect.size);
    [image drawInRect:rect];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return scaledImage;
  }
  
  return image;
}

- (CGFloat)findFirstLevelQualityWithImage:(UIImage *)image maxFileSize:(NSInteger)maxFileSize {
  CGFloat quality = 1;
  NSData *originalData = UIImageJPEGRepresentation(image, quality);
  
  NSData *compressedData = originalData;
  while (compressedData.length > maxFileSize) {
    quality -= [self linearCommonDifference];
    if (quality <= 0) {
      break;
    }
    
    compressedData = UIImageJPEGRepresentation(image, quality);
  }
  return quality;
}

- (NSData *)binaryCompress:(UIImage *)image
          firstLevelQuality:(CGFloat)firstLevelQuality
                maxFileSize:(NSInteger)maxFileSize {
  NSData *originalData = UIImageJPEGRepresentation(image, firstLevelQuality);
  
  CGFloat maxQuality = firstLevelQuality + [self linearCommonDifference];
  if (maxQuality > 1) {
    return originalData;
  }
  
  if (originalData.length == maxFileSize) {
    return originalData;
  }
  
  CGFloat currentMaxQuality = maxQuality;
  CGFloat currentMinQuality = firstLevelQuality;
  NSInteger binarySearchCount = 2;
  NSData *data = originalData;
  for (NSInteger index = 0; index < binarySearchCount; index++) {
    CGFloat quality = (currentMaxQuality + currentMinQuality) / 2.0;
    data = UIImageJPEGRepresentation(image, quality);
    if (data.length < maxFileSize) {
      currentMinQuality = quality;
    } else if (data.length > maxFileSize) {
      currentMaxQuality = quality;
    } else {
      break;
    }
  }

  return data;
}

- (CGFloat)linearCommonDifference {
  return 0.1;
}

@end
