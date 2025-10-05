package com.back_hexiang_studio.utils;

import com.back_hexiang_studio.enumeration.FileType;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;




import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件处理工具类
 * <p>
 * 封装了文件上传、删除和URL获取等核心静态方法。
 * </p>
 *
 * @author Gemini
 * @since 2024-07-12
 */
@Component
public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);
    /**
     * 文件在服务器上存储的根目录。
     * 从 application.yml 的 `file.upload.path` 注入。
     */
    private static String UPLOAD_BASE_PATH;



    // Spring无法直接注入静态变量，因此先注入到实例变量中
    @Value("${file.upload.path:upload}")
    private String uploadBasePath;

    @Value("${file.access.url:/upload}")
    private String fileAccessUrl;

    /**
     * Spring Bean初始化后执行此方法。
     * 用于将从配置文件注入的实例变量值赋给静态变量。
     */
    @PostConstruct
    public void init() {
        UPLOAD_BASE_PATH = uploadBasePath;
        System.out.println("初始化文件上传配置：");
        System.out.println("上传根路径: " + UPLOAD_BASE_PATH);
        createDirectoryIfNotExists(UPLOAD_BASE_PATH);
    }

    /**
     * 用于创建目录（如果它不存在）。
     * @param dirPath 要创建的目录的完整物理路径。
     */
    private static void createDirectoryIfNotExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 通用的文件保存方法。
     * <p>
     * 根据传入的 {@link FileType}，将上传的文件保存到模块化、按日期组织的子目录中。
     * 例如：{UPLOAD_BASE_PATH}/notice/image/2024/07/12/uuid.jpg
     * </p>
     *
     * @param file     前端上传的 {@link MultipartFile} 文件对象。
     * @param fileType 文件的业务类型，定义在 {@link FileType} 枚举中。
     * @return 文件相对于存储根目录的相对路径，例如 "notice/image/2024/07/12/uuid.jpg"。
     * @throws IOException 当文件读写发生错误时抛出。
     */
    public static String saveFile(MultipartFile file, FileType fileType) throws IOException {

        // 从枚举获取业务模块子路径, e.g., "notice/image"
        String modulePath = fileType.getPath();
        // 获取当前日期路径, e.g., "2024/07/12"
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 构造完整的物理目标目录
        Path targetDir = Paths.get(UPLOAD_BASE_PATH, modulePath, dateDir);
        // 如果目录不存在，则递归创建
        Files.createDirectories(targetDir);

        // 生成一个包含UUID的唯一文件名，以避免冲突
        String newFileName = generateFilename(file.getOriginalFilename());
        // 将文件流复制到目标路径
        Files.copy(file.getInputStream(), targetDir.resolve(newFileName));

        // 返回用于数据库存储和外部访问的相对路径
        return Paths.get(modulePath, dateDir, newFileName).toString().replace("\\", "/");
    }


    /**
     * 将临时文件移动到其最终的永久存储目录。
     *
     * @param tempRelativePath 临时文件的相对路径 (e.g., "temp/2024/07/15/uuid.ext")
     * @param finalFileType    文件的最终业务类型，用于确定其永久存储的模块路径 (e.g., FileType.AVATAR_TEACHER)
     * @return 新的文件相对路径 (e.g., "avatar/teacher/2024/07/15/uuid.ext")
     * @throws IOException 如果文件移动过程中发生IO错误
     */
    public static String moveFileToPermanentDirectory(String tempRelativePath, FileType finalFileType) throws IOException {
        Path tempAbsolutePath = Paths.get(UPLOAD_BASE_PATH, tempRelativePath);

        // 如果临时文件不存在，直接返回一个空字符串或抛出异常，表示操作失败
        if (Files.notExists(tempAbsolutePath)) {
            log.warn("尝试移动一个不存在的临时文件: {}", tempAbsolutePath);
            throw new IOException("要移动的临时文件不存在: " + tempRelativePath);
        }

        // 从临时路径中解析出文件名
        String fileName = Paths.get(tempRelativePath).getFileName().toString();

        // 构建新的永久存储路径
        String finalModulePath = finalFileType.getPath();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path finalRelativePathObj = Paths.get(finalModulePath, dateDir, fileName);
        Path finalAbsolutePath = Paths.get(UPLOAD_BASE_PATH).resolve(finalRelativePathObj);

        // 创建新目录并移动文件
        Files.createDirectories(finalAbsolutePath.getParent());
        Files.move(tempAbsolutePath, finalAbsolutePath);

        log.info("成功将文件从临时目录 {} 移动到 {}", tempAbsolutePath, finalAbsolutePath);

        // 返回新的相对路径，并确保使用正斜杠
        return finalRelativePathObj.toString().replace("\\", "/");
    }


    /**
     * 上传文件（根据资料分类）
     * @param file
     * @param categoryName
     * @return
     * @throws IOException
     */
    public static String saveFile(MultipartFile file, String categoryName) throws IOException {

        // 从枚举获取业务模块子路径, e.g., "notice/image"
        String modulePath = "material/"+categoryName;
        //生成时间路径
        String dateDir=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        //构造完整路径
        Path targetDir=Paths.get(UPLOAD_BASE_PATH,modulePath,dateDir);

        //如果目录不存在，则递归创建
        Files.createDirectories(targetDir);
        //生成唯一UUID
        String newFileName=generateFilename(file.getOriginalFilename());

        //保存到路径下
        Files.copy(file.getInputStream(), targetDir.resolve(newFileName));

        //返回完整路径
        return Paths.get(modulePath,dateDir,newFileName).toString().replace("\\", "/");


    }


    /**
     * 当资料分类更新时，移动物理文件到新的分类目录下。
     *
     * @param oldRelativePath 数据库中存储的旧的相对路径 (e.g., "material/旧分类/2024/07/15/file.ext")
     * @param newCategoryName 新的分类名称
     * @return 新的文件相对路径 (e.g., "material/新分类/2024/07/15/file.ext")
     * @throws IOException 如果文件移动过程中发生IO错误
     */
    public static String moveMaterialFile(String oldRelativePath, String newCategoryName) throws IOException {
        Path oldAbsolutePath = Paths.get(UPLOAD_BASE_PATH, oldRelativePath);

        // 如果原始文件不存在，直接返回旧路径，不进行任何操作
        if (Files.notExists(oldAbsolutePath)) {
            log.warn("Attempted to move a non-existent file: {}", oldAbsolutePath);
            return oldRelativePath;
        }

        // 从旧的相对路径中解析出日期和文件名部分
        Path oldRelativePathObj = Paths.get(oldRelativePath);
        // 路径结构: material/{category}/{yyyy}/{MM}/{dd}/{filename} -> 5层
        if (oldRelativePathObj.getNameCount() < 5) {
            log.error("Invalid material path structure, cannot move file: {}", oldRelativePath);
            throw new IOException("路径结构不合法，无法移动文件。");
        }
        Path dateAndFileNamePath = oldRelativePathObj.subpath(2, oldRelativePathObj.getNameCount());

        // 构建新的相对路径和绝对路径
        Path newRelativePathObj = Paths.get("material", newCategoryName).resolve(dateAndFileNamePath);
        Path newAbsolutePath = Paths.get(UPLOAD_BASE_PATH).resolve(newRelativePathObj);

        // 创建新目录并移动文件
        Files.createDirectories(newAbsolutePath.getParent());
        Files.move(oldAbsolutePath, newAbsolutePath);

        log.info("Successfully moved file from {} to {}", oldAbsolutePath, newAbsolutePath);

        // 返回新的相对路径，并确保使用正斜杠
        return newRelativePathObj.toString().replace("\\", "/");
    }


    /**
     * 生成UUID
     * @param originalFilename 原始文件名，例如 "my-photo.jpg"。
     * @return 转换后的唯一文件名，例如 "uuid-string.jpg"。
     */
    private static String generateFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 从服务器磁盘上删除一个物理文件。
     *
     * @param filePath 要删除的文件的相对路径 (由 saveFile 方法生成)。
     * @return 如果删除成功或文件不存在，则返回 true；如果删除失败，则返回 false。
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        try {
            // 将相对路径转换为服务器上的绝对物理路径
            Path path = Paths.get(UPLOAD_BASE_PATH + File.separator + filePath.replace("/", File.separator));
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("删除文件失败: " + filePath + " - " + e.getMessage());
            return false;
        }
    }


} 