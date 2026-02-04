package com.portfolio.backend.exceptions;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(Long assetId) {
        super("Asset not found with ID: " + assetId);
    }
}
