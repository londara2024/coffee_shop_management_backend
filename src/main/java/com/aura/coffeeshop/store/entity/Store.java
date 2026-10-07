package com.aura.coffeeshop.store.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 250)
    private String address;

    @Column(name = "hours_label", length = 100)
    private String hoursLabel;

    /** Map location; both null or both set. */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    /** Open/closed switch from the admin settings page. Checkout is refused while closed. */
    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    /** Uploaded shop logo; null = the default Aura mark. */
    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    /** Theme colour ({@code #rrggbb}), one of {@code brand_colors.hex}. */
    @Column(name = "brand_color", nullable = false, length = 7)
    private String brandColor = "#8d4f06";

    public String mapsUrl() {
        return latitude == null || longitude == null ? null
                : "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
    }
}
