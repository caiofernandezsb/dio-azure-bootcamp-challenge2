package org.dio.bootcamp.azuredocumentanalysis;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@SpringBootApplication
public class AzureDocumentAnalysisApplication {

    private final String storageConnectionString = "";
    private final String storageContainerName = "cartoes";
    private final String endpoint = "https://dio-lab-document-intelligence01.cognitiveservices.azure.com/";
    private final String apiKey = "";

    private final String filePath = "/Users/caiofsbarros/Downloads/IMG_1759.jpeg";
    private final String blobName = "IMG_1759.jpg";

    public static void main(String[] args) {
        SpringApplication.run(AzureDocumentAnalysisApplication.class, args);
        AzureDocumentAnalysisApplication app = new AzureDocumentAnalysisApplication();
        try {
            app.uploadAndAnalyze();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void uploadAndAnalyze() throws IOException {
        String blobUrl = uploadImage();
        System.out.println("Uploaded Blob URL: " + blobUrl);
        analyzeCreditCard(blobUrl);
    }

    public String uploadImage() throws IOException {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(storageContainerName)
                .buildClient();

        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            blobClient.upload(fileInputStream, file.length(), true);
            System.out.println("Upload successful. Blob URL: " + blobClient.getBlobUrl());
            return blobClient.getBlobUrl();
        }
    }

    public void analyzeCreditCard(String blobUrl) {
        DocumentAnalysisClient documentAnalysisClient = new DocumentAnalysisClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();


        //TODO ---> find a lib version compatible with credit card prebuilt model
        SyncPoller<OperationResult, AnalyzeResult> analyzeDocumentPoller =
                documentAnalysisClient.beginAnalyzeDocumentFromUrl("prebuilt-document", blobUrl);

        AnalyzeResult analyzeResult = analyzeDocumentPoller.getFinalResult();

        if (analyzeResult.getDocuments() == null || analyzeResult.getDocuments().isEmpty()) {
            System.out.println("No structured documents were detected in the image.");

            if (analyzeResult.getPages() != null && !analyzeResult.getPages().isEmpty()) {
                System.out.println("Text detected at page level:");
                for (DocumentPage page : analyzeResult.getPages()) {
                    System.out.printf("Page %d, width: %.2f, height: %.2f%n",
                            page.getPageNumber(), page.getWidth(), page.getHeight());

                    if (page.getLines() != null) {
                        for (DocumentLine line : page.getLines()) {
                            System.out.printf("Line: %s%n", line.getContent());
                        }
                    }
                }
            } else {
                System.out.println("No text detected in the image.");
            }
            return;
        }

        System.out.println("Document Analysis Results:");
        for (AnalyzedDocument document : analyzeResult.getDocuments()) {
            System.out.println("---- Analyzed Document ----");
            for (String fieldName : document.getFields().keySet()) {
                DocumentField field = document.getFields().get(fieldName);
                if (field != null) {
                    System.out.printf("Field: %s, Value: %s, Confidence: %.2f%n",
                            fieldName, field.getValueAsString(), field.getConfidence());
                }
            }
        }
    }
}