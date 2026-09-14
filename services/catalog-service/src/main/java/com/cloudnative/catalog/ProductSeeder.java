package com.cloudnative.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cloudnative.catalog.domain.Product;
import com.cloudnative.catalog.repo.ProductRepository;

@Component
public class ProductSeeder implements CommandLineRunner {

  private final ProductRepository products;

  public ProductSeeder(ProductRepository products) {
    this.products = products;
  }

  @Override
  public void run(String... args) {
    if (products.count() > 0) {
      return;
    }

    products.saveAll(List.of(
        product("iPhone 16 Pro 256 GB", "Pantalla Super Retina XDR, chip A18 Pro y cámara de 48 MP.",
            "CELULARES", "Apple", 1299990, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80", 12),
        product("Samsung Galaxy S25 Ultra", "Galaxy AI, zoom espacial y S Pen integrado.",
            "CELULARES", "Samsung", 1199990, "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&w=900&q=80", 15),
        product("Google Pixel 9 Pro", "Fotografía computacional y Android puro.",
            "CELULARES", "Google", 899990, "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80", 10),
        product("MacBook Air 13\" M3", "Hasta 18 horas de batería y chasis de aluminio.",
            "COMPUTADORAS", "Apple", 1499990, "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=80", 8),
        product("Dell XPS 15 OLED", "Pantalla 3.5K OLED táctil y Core Ultra 7.",
            "COMPUTADORAS", "Dell", 1799990, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80", 6),
        product("ASUS ROG Strix G16", "Notebook gamer RTX 4070 y teclado RGB.",
            "COMPUTADORAS", "ASUS", 1899990, "https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=900&q=80", 5),
        product("Samsung Neo QLED 65\" 4K", "Mini LED, 120 Hz y Gaming Hub.",
            "TELEVISORES", "Samsung", 899990, "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=900&q=80", 7),
        product("LG OLED C4 55\"", "Negros perfectos, webOS y 144 Hz para consolas.",
            "TELEVISORES", "LG", 1099990, "https://images.unsplash.com/photo-1461151304267-38535e780c79?auto=format&fit=crop&w=900&q=80", 4),
        product("PlayStation 5 Slim", "1 TB, DualSense y catálogo exclusivo.",
            "CONSOLAS", "Sony", 649990, "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?auto=format&fit=crop&w=900&q=80", 9),
        product("Xbox Series X", "4K nativo, Quick Resume y Game Pass.",
            "CONSOLAS", "Microsoft", 599990, "https://images.unsplash.com/photo-1621259182978-fbf93132d53d?auto=format&fit=crop&w=900&q=80", 8),
        product("Nintendo Switch OLED", "Pantalla OLED de 7\" y dock con LAN.",
            "CONSOLAS", "Nintendo", 399990, "https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?auto=format&fit=crop&w=900&q=80", 14),
        product("iPad Air 13\" M2", "Pantalla Liquid Retina y Apple Pencil Pro.",
            "ACCESORIOS", "Apple", 749990, "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=900&q=80", 11),
        product("AirPods Pro 2", "Cancelación activa de ruido y estuche USB-C.",
            "ACCESORIOS", "Apple", 249990, "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?auto=format&fit=crop&w=900&q=80", 20),
        product("Logitech MX Master 3S", "Sensor 8K y recarga USB-C para productividad.",
            "ACCESORIOS", "Logitech", 99990, "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=900&q=80", 25),
        product("Monitor LG UltraFine 27\" 4K", "IPS, HDR10 y USB-C con 90 W.",
            "ACCESORIOS", "LG", 449990, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80", 9),
        product("Keychron K2 V2", "Teclado mecánico inalámbrico RGB hot-swap.",
            "ACCESORIOS", "Keychron", 129990, "https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=900&q=80", 18)
    ));
  }

  private Product product(String name, String description, String category, String brand, int price, String imageUrl, int stock) {
    Product item = new Product();
    item.setName(name);
    item.setDescription(description);
    item.setCategory(category);
    item.setBrand(brand);
    item.setPrice(BigDecimal.valueOf(price));
    item.setImageUrl(imageUrl);
    item.setStock(stock);
    return item;
  }
}
