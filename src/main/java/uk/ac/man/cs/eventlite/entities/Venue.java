package uk.ac.man.cs.eventlite.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.OneToMany;

@Entity
@Table(name = "venues")
public class Venue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //neccesarry annotations
    private long id;
    
    @JsonIgnore
    @OneToMany(mappedBy = "venue")
    private List<Event> events;

    @NotBlank(message = "Name cannot be empty.")
    @Size(max = 255, message = "Name must be less than 256 characters.")
    private String name;
    
    @NotBlank(message = "Address cannot be empty.")
    @Size(max = 299, message = "Address must be less than 300 characters.")
    @Pattern(regexp = ".*\\b[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}\\b.*", 
             message = "Address must contain a valid UK postcode.")
    private String address;

    @Positive(message = "Capacity must be a positive integer.")
    private int capacity;

    //mapbox fields
    private double longitude;
    private double latitude;

    public Venue() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    //mapbox funcs
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}