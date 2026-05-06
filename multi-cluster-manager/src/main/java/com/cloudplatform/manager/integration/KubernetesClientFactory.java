package com.cloudplatform.manager.integration;

import com.cloudplatform.manager.model.entity.Cluster;
import com.cloudplatform.manager.util.EncryptionUtil;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KubernetesClientFactory {
    private final ConcurrentHashMap<Long, KubernetesClient> clientCache = new ConcurrentHashMap<>();
    @Autowired private EncryptionUtil encryptionUtil;

    public KubernetesClient getClient(Cluster cluster) {
        return clientCache.computeIfAbsent(cluster.getId(), id -> {
            String decryptedKubeconfig = encryptionUtil.decrypt(cluster.getKubeconfigEncrypted());
            // 额外证书可选
            Config config = Config.fromKubeconfig(decryptedKubeconfig);
            return new DefaultKubernetesClient(config);
        });
    }

    public void evictClient(Long clusterId) {
        KubernetesClient client = clientCache.remove(clusterId);
        if (client != null) {
            client.close();
        }
    }
}