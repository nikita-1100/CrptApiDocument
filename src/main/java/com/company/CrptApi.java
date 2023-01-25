package com.company;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final int requestLimit;
    private static int counter;
    private final long interval;
    private static long firstRequestTime = 0;
    private final static String TOKEN = "token";
    private final static ProductGroup PG = ProductGroup.milk;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        interval = timeUnit.toNanos(1);
        this.requestLimit = requestLimit;
    }

    public void sendDocumentRequest(Documentable document, String signature) {
        synchronized (this) {
            if (counter == requestLimit) {
                long lastRequestTime = System.nanoTime();
                long res = lastRequestTime - firstRequestTime;
                try {
                    Thread.sleep(res > interval ? 0 : (interval - res) / 1000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter = 1;
            }

            if (counter == 1) {
                firstRequestTime = System.nanoTime();
            }
            counter++;
            sendHttpRequest(document, signature);
        }
    }


    private static final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private String mapToBase64(Object obj) {
        try {
            return new String(Base64.encodeBase64(mapper.writeValueAsString(obj).getBytes()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendHttpRequest(Documentable document, String signature) {
        final CloseableHttpClient httpclient = HttpClients.createDefault();

        final HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create?pg=" + PG);

        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("document_format", DocumentFormat.MANUAL.toString()));
        params.add(new BasicNameValuePair("product_document", mapToBase64(document)));
        params.add(new BasicNameValuePair("product_group", PG.toString()));
        params.add(new BasicNameValuePair("signature", mapToBase64(signature)));
        params.add(new BasicNameValuePair("type", document.getDocumentType()));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.addHeader("'content-type", " application/json");
            httpPost.addHeader("Authorization", "Bearer " + TOKEN);
            CloseableHttpResponse response2 = httpclient.execute(httpPost);
            final HttpEntity entity = response2.getEntity();
            System.out.println(EntityUtils.toString(entity));
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    enum DocumentFormat {
        MANUAL,
        XML,
        CSV
    }

    enum ProductGroup {
        clothes,
        shoes,
        tobacco,
        perfumery,
        tires,
        electronics,
        pharma,
        milk,
        bicycle,
        wheelchairs
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class Description {
        private String participantInn;
    }

    interface Documentable {
        String getDocumentType();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class LpIntroduceGoods implements Documentable {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String production_type;
        private List<Product> products;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate reg_date;
        private String reg_number;

        @Override
        @JsonIgnore
        public String getDocumentType() {
            return "LP_INTRODUCE_GOODS";
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class Product {
        private String certificate_document;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
