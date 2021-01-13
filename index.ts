import { NativeModules, Platform } from 'react-native';

/** 文件压缩参数 */
export interface ImageCompressParams {
  /** 图片文件路径 */
  imageFileUri: string;
  /** 最大高或者宽，如：5000 */
  maxWidthOrHeight: number;
  /** 文件最大限制，如 5MB，传5 * 1024 * 1024 */
  maxFileSize: number;
}

class ImageCompressUtil {
  /**
   * 压缩图片
   */
  compressImage = async (
    params: ImageCompressParams
  ): Promise<{ compressedUri: string }> => {
    const Module =
      Platform.OS === 'ios'
        ? NativeModules.YTImageCompress
        : NativeModules.ImageCompressModule;
    return Module.compressImage(params);
  };
}

export const imageCompressUtil = new ImageCompressUtil();
