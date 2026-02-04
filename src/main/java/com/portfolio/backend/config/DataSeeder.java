package com.portfolio.backend.config;

import com.portfolio.backend.entity.Asset;
import com.portfolio.backend.entity.enums.AssetCategory;
import com.portfolio.backend.repository.AssetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AssetRepository assetRepository;

    public DataSeeder(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        
        // 1. Check if the database is already populated to avoid duplicates
        if (assetRepository.count() > 0) {
            System.out.println("✅ Assets already loaded. Skipping seeding.");
            return;
        }

        System.out.println("🌱 Seeding initial assets...");

        // 2. Define the Assets (5 NSE, 5 MF, 3 Commodity)
        List<Asset> assets = Arrays.asList(
            // --- NSE Stocks (Limit 5) ---
            new Asset(null, "RELIANCE", "Reliance Industries", AssetCategory.NSE, "Oil, Gas, and Telecom giant"),
            new Asset(null, "TCS", "Tata Consultancy Services", AssetCategory.NSE, "IT Services"),
            new Asset(null, "HDFCBANK", "HDFC Bank", AssetCategory.NSE, "Banking and Finance"),
            new Asset(null, "INFY", "Infosys", AssetCategory.NSE, "IT Services"),
            new Asset(null, "ICICIBANK", "ICICI Bank", AssetCategory.NSE, "Banking and Finance"),

            // --- Mutual Funds (Limit 5) ---
            new Asset(null, "SBI_BLUECHIP", "SBI Bluechip Fund", AssetCategory.MF, "Large Cap Fund"),
            new Asset(null, "HDFC_BALANCED", "HDFC Balanced Advantage", AssetCategory.MF, "Hybrid Fund"),
            new Asset(null, "AXIS_LONGTERM", "Axis Long Term Equity", AssetCategory.MF, "ELSS Tax Saver"),
            new Asset(null, "ICICI_TECH", "ICICI Prudential Technology", AssetCategory.MF, "Sectoral IT Fund"),
            new Asset(null, "MOTILAL_MIDCAP", "Motilal Oswal Midcap", AssetCategory.MF, "Mid Cap Fund"),

            // --- Commodities (Specific 3) ---
            new Asset(null, "GOLD", "Gold 24k", AssetCategory.COMMODITY, "Precious Metal"),
            new Asset(null, "SILVER", "Silver", AssetCategory.COMMODITY, "Precious Metal"),
            new Asset(null, "COPPER", "Copper", AssetCategory.COMMODITY, "Industrial Metal")
        );

        // 3. Save all to Database
        assetRepository.saveAll(assets);
        System.out.println("✅ Successfully seeded " + assets.size() + " assets!");
    }
}