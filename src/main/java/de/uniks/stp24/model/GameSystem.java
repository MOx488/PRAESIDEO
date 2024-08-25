package de.uniks.stp24.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameSystem {
    @JsonProperty("createdAt")
    private String createdAt;
    @JsonProperty("updatedAt")
    private String updatedAt;
    @JsonProperty("_id")
    private String _id;
    @JsonProperty("game")
    private String game;
    @JsonProperty("type")
    private String type;
    @JsonProperty("name")
    private String name;
    @JsonProperty("health")
    private double health;
    @JsonProperty("districtSlots")
    private TreeMap<String, Integer> districtSlots;
    @JsonProperty("districts")
    private TreeMap<String, Integer> districts;
    @JsonProperty("capacity")
    private int capacity;
    @JsonProperty("buildings")
    private List<String> buildings;
    @JsonProperty("upgrade")
    private String upgrade;
    @JsonProperty("population")
    private int population;
    @JsonProperty("links")
    private Map<String, Double> links;
    @JsonProperty("x")
    private int x;
    @JsonProperty("y")
    private int y;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("_public")
    private Map<String, Object> _public;

    public GameSystem() {
    }

    public GameSystem(String createdAt, String updatedAt, String _id, String game, String type, String name, Double health, TreeMap<String, Integer> districtSlots, TreeMap<String, Integer> districts, int capacity, List<String> buildings, String upgrade, int population, Map<String, Double> links, int x, int y, String owner, Map<String, Object> _public) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this._id = _id;
        this.game = game;
        this.type = type;
        this.name = name;
        this.districtSlots = districtSlots;
        this.districts = districts;
        this.capacity = capacity;
        this.buildings = buildings;
        this.upgrade = upgrade;
        this.population = population;
        this.links = links;
        this.x = x;
        this.y = y;
        this.owner = owner;
        this._public = _public;
        this.health = health;
    }

    public String createdAt() {
        return createdAt;
    }

    public GameSystem setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public GameSystem setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String _id() {
        return _id;
    }

    public GameSystem set_id(String _id) {
        this._id = _id;
        return this;
    }

    public String game() {
        return game;
    }

    public GameSystem setGame(String game) {
        this.game = game;
        return this;
    }

    public String type() {
        return type;
    }

    public GameSystem setType(String type) {
        this.type = type;
        return this;
    }

    public String name() {
        return name;
    }

    public GameSystem setName(String name) {
        this.name = name;
        return this;
    }

    public double health() {
        return health;
    }

    public GameSystem setHealth(double health) {
        this.health = health;
        return this;
    }

    public TreeMap<String, Integer> districtSlots() {
        return districtSlots;
    }

    public GameSystem setDistrictSlots(TreeMap<String, Integer> districtSlots) {
        this.districtSlots = districtSlots;
        return this;
    }

    public TreeMap<String, Integer> districts() {
        return districts;
    }

    public GameSystem setDistricts(TreeMap<String, Integer> districts) {
        this.districts = districts;
        return this;
    }

    public int capacity() {
        return capacity;
    }

    public GameSystem setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public List<String> buildings() {
        return buildings;
    }

    public GameSystem setBuildings(List<String> buildings) {
        this.buildings = buildings;
        return this;
    }

    public String upgrade() {
        return upgrade;
    }

    public GameSystem setUpgrade(String upgrade) {
        this.upgrade = upgrade;
        return this;
    }

    public int population() {
        return population;
    }

    public GameSystem setPopulation(int population) {
        this.population = population;
        return this;
    }

    public Map<String, Double> links() {
        return links;
    }

    public GameSystem setLinks(Map<String, Double> links) {
        this.links = links;
        return this;
    }

    public int x() {
        return x;
    }

    public GameSystem setX(int x) {
        this.x = x;
        return this;
    }

    public int y() {
        return y;
    }

    public GameSystem setY(int y) {
        this.y = y;
        return this;
    }

    public String owner() {
        return owner;
    }

    public GameSystem setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Map<String, Object> _public() {
        return _public;
    }

    public GameSystem set_public(Map<String, Object> _public) {
        this._public = _public;
        return this;
    }
}
