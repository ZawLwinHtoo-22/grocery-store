package com.example.grocerystore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@org.springframework.beans.factory.annotation.Value("${cloudinary.cloud-name:}") String cloudName,
                             @org.springframework.beans.factory.annotation.Value("${cloudinary.api-key:}") String apiKey,
                             @org.springframework.beans.factory.annotation.Value("${cloudinary.api-secret:}") String apiSecret) {
        // Prefer use of env vars or application properties. If none provided, Cloudinary will throw on use.
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        Object url = uploadResult.get("secure_url");
        return url == null ? uploadResult.get("url").toString() : url.toString();
    }
}