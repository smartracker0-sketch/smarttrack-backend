package com.trackpro.service;

import com.trackpro.exception.NotFoundException;
import com.trackpro.model.*;
import com.trackpro.repository.*;
import com.trackpro.security.CurrentUser;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Single service covering all fleet sub-modules:
 * maintenance, expenses, documents, vendors, consigners, elock, tyres.
 *
 * Org-scoping: each record is associated with the current user's organisation.
 * Users without an organisation can still create records (org is null — personal/unassigned).
 */
@Service
public class FleetService {

    private final UserRepository        userRepo;
    private final OrganisationRepository orgRepo;
    private final DeviceRepository      deviceRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final ExpenseRepository     expenseRepo;
    private final DocumentRepository    documentRepo;
    private final VendorRepository      vendorRepo;
    private final ConsignerRepository   consignerRepo;
    private final ElockRepository       elockRepo;
    private final TyreRepository        tyreRepo;

    public FleetService(
            UserRepository userRepo,
            OrganisationRepository orgRepo,
            DeviceRepository deviceRepo,
            MaintenanceRepository maintenanceRepo,
            ExpenseRepository expenseRepo,
            DocumentRepository documentRepo,
            VendorRepository vendorRepo,
            ConsignerRepository consignerRepo,
            ElockRepository elockRepo,
            TyreRepository tyreRepo) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.deviceRepo = deviceRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.expenseRepo = expenseRepo;
        this.documentRepo = documentRepo;
        this.vendorRepo = vendorRepo;
        this.consignerRepo = consignerRepo;
        this.elockRepo = elockRepo;
        this.tyreRepo = tyreRepo;
    }

    // ── helpers ────────────────────────────────────────────────────

    private UserEntity currentUser() {
        return userRepo.findById(CurrentUser.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private OrganisationEntity currentOrg(UserEntity u) {
        return u.getOrganisation();
    }

    private UUID currentOrgId() {
        var u = currentUser();
        var org = currentOrg(u);
        return org == null ? null : org.getId();
    }

    private DeviceEntity resolveDevice(UUID deviceId) {
        if (deviceId == null) return null;
        return deviceRepo.findById(deviceId).orElse(null);
    }

    // ── Maintenance ────────────────────────────────────────────────

    public Page<MaintenanceRecord> listMaintenance(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return maintenanceRepo.findByOrganisationIdOrderByDueDateAsc(orgId, pageable);
        return maintenanceRepo.findByOrganisationIsNullOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public MaintenanceRecord createMaintenance(MaintenanceRequest req) {
        var user = currentUser();
        var record = new MaintenanceRecord();
        record.setOrganisation(currentOrg(user));
        record.setDevice(resolveDevice(req.deviceId()));
        record.setVehiclePlate(req.vehiclePlate());
        record.setTask(req.task());
        record.setStatus(req.status() != null ? req.status() : "PENDING");
        record.setDueDate(req.dueDate());
        record.setCost(req.cost());
        record.setNotes(req.notes());
        record.setCreatedBy(user);
        return maintenanceRepo.save(record);
    }

    @Transactional
    public MaintenanceRecord updateMaintenance(UUID id, MaintenanceRequest req) {
        var record = maintenanceRepo.findById(id).orElseThrow(() -> new NotFoundException("Record not found"));
        record.setTask(req.task());
        if (req.status() != null) record.setStatus(req.status());
        record.setDueDate(req.dueDate());
        record.setCost(req.cost());
        record.setNotes(req.notes());
        if (req.vehiclePlate() != null) record.setVehiclePlate(req.vehiclePlate());
        return maintenanceRepo.save(record);
    }

    @Transactional
    public void deleteMaintenance(UUID id) {
        maintenanceRepo.deleteById(id);
    }

    public record MaintenanceRequest(
            UUID deviceId, String vehiclePlate, String task, String status,
            java.time.LocalDate dueDate, java.math.BigDecimal cost, String notes) {}

    // ── Expenses ───────────────────────────────────────────────────

    public Page<Expense> listExpenses(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return expenseRepo.findByOrganisationIdOrderByExpenseDateDesc(orgId, pageable);
        return expenseRepo.findByOrganisationIsNullOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Expense createExpense(ExpenseRequest req) {
        var user = currentUser();
        var e = new Expense();
        e.setOrganisation(currentOrg(user));
        e.setDevice(resolveDevice(req.deviceId()));
        e.setVehiclePlate(req.vehiclePlate());
        e.setCategory(req.category() != null ? req.category() : "OTHER");
        e.setAmount(req.amount());
        e.setCurrency(req.currency() != null ? req.currency() : "USD");
        e.setDescription(req.description());
        e.setExpenseDate(req.expenseDate() != null ? req.expenseDate() : java.time.LocalDate.now());
        e.setCreatedBy(user);
        return expenseRepo.save(e);
    }

    @Transactional
    public void deleteExpense(UUID id) { expenseRepo.deleteById(id); }

    public record ExpenseRequest(
            UUID deviceId, String vehiclePlate, String category,
            java.math.BigDecimal amount, String currency,
            String description, java.time.LocalDate expenseDate) {}

    // ── Documents ──────────────────────────────────────────────────

    public Page<Document> listDocuments(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return documentRepo.findByOrganisationIdOrderByExpiryDateAsc(orgId, pageable);
        return documentRepo.findByOrganisationIsNullOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public Document createDocument(DocumentRequest req) {
        var user = currentUser();
        var d = new Document();
        d.setOrganisation(currentOrg(user));
        d.setDevice(resolveDevice(req.deviceId()));
        d.setVehiclePlate(req.vehiclePlate());
        d.setDocType(req.docType() != null ? req.docType() : "OTHER");
        d.setTitle(req.title());
        d.setFileUrl(req.fileUrl());
        d.setExpiryDate(req.expiryDate());
        // auto-compute status from expiry
        if (req.expiryDate() != null) {
            var today = java.time.LocalDate.now();
            if (req.expiryDate().isBefore(today)) d.setStatus("EXPIRED");
            else if (req.expiryDate().isBefore(today.plusDays(30))) d.setStatus("EXPIRING_SOON");
            else d.setStatus("VALID");
        }
        d.setCreatedBy(user);
        return documentRepo.save(d);
    }

    @Transactional
    public void deleteDocument(UUID id) { documentRepo.deleteById(id); }

    public record DocumentRequest(
            UUID deviceId, String vehiclePlate, String docType,
            String title, String fileUrl, java.time.LocalDate expiryDate) {}

    // ── Vendors ────────────────────────────────────────────────────

    public Page<Vendor> listVendors(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return vendorRepo.findByOrganisationIdAndActiveTrueOrderByNameAsc(orgId, pageable);
        return vendorRepo.findByOrganisationIsNullOrderByNameAsc(pageable);
    }

    @Transactional
    public Vendor createVendor(VendorRequest req) {
        var user = currentUser();
        var v = new Vendor();
        v.setOrganisation(currentOrg(user));
        v.setName(req.name());
        v.setVendorType(req.vendorType() != null ? req.vendorType() : "OTHER");
        v.setContactName(req.contactName());
        v.setContactPhone(req.contactPhone());
        v.setContactEmail(req.contactEmail());
        v.setAddress(req.address());
        v.setCreatedBy(user);
        return vendorRepo.save(v);
    }

    @Transactional
    public void deleteVendor(UUID id) { vendorRepo.deleteById(id); }

    public record VendorRequest(
            String name, String vendorType, String contactName,
            String contactPhone, String contactEmail, String address) {}

    // ── Consigners ─────────────────────────────────────────────────

    public Page<Consigner> listConsigners(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return consignerRepo.findByOrganisationIdAndActiveTrueOrderByNameAsc(orgId, pageable);
        return consignerRepo.findByOrganisationIsNullOrderByNameAsc(pageable);
    }

    @Transactional
    public Consigner createConsigner(ConsignerRequest req) {
        var user = currentUser();
        var c = new Consigner();
        c.setOrganisation(currentOrg(user));
        c.setName(req.name());
        c.setConsignerType(req.consignerType() != null ? req.consignerType() : "CONSIGNOR");
        c.setContactPhone(req.contactPhone());
        c.setContactEmail(req.contactEmail());
        c.setAddress(req.address());
        c.setCreatedBy(user);
        return consignerRepo.save(c);
    }

    @Transactional
    public void deleteConsigner(UUID id) { consignerRepo.deleteById(id); }

    public record ConsignerRequest(
            String name, String consignerType,
            String contactPhone, String contactEmail, String address) {}

    // ── E-Lock ─────────────────────────────────────────────────────

    public Page<ElockDevice> listElocks(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return elockRepo.findByOrganisationIdOrderByCreatedAtDesc(orgId, pageable);
        return elockRepo.findByOrganisationIsNullOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public ElockDevice createElock(ElockRequest req) {
        var user = currentUser();
        var e = new ElockDevice();
        e.setOrganisation(currentOrg(user));
        e.setDevice(resolveDevice(req.deviceId()));
        e.setLockSerial(req.lockSerial());
        e.setStatus(req.status() != null ? req.status() : "UNKNOWN");
        e.setNotes(req.notes());
        return elockRepo.save(e);
    }

    @Transactional
    public ElockDevice patchElockStatus(UUID id, String status) {
        var e = elockRepo.findById(id).orElseThrow(() -> new NotFoundException("E-lock not found"));
        e.setStatus(status);
        e.setLastEventAt(java.time.Instant.now());
        return elockRepo.save(e);
    }

    @Transactional
    public void deleteElock(UUID id) { elockRepo.deleteById(id); }

    public record ElockRequest(UUID deviceId, String lockSerial, String status, String notes) {}

    // ── Tyres ──────────────────────────────────────────────────────

    public Page<TyreRecord> listTyres(Pageable pageable) {
        var orgId = currentOrgId();
        if (orgId != null) return tyreRepo.findByOrganisationIdOrderByCreatedAtDesc(orgId, pageable);
        return tyreRepo.findByOrganisationIsNullOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public TyreRecord createTyre(TyreRequest req) {
        var user = currentUser();
        var t = new TyreRecord();
        t.setOrganisation(currentOrg(user));
        t.setDevice(resolveDevice(req.deviceId()));
        t.setVehiclePlate(req.vehiclePlate());
        t.setPosition(req.position());
        t.setBrand(req.brand());
        t.setSerialNo(req.serialNo());
        t.setStatus(req.status() != null ? req.status() : "GOOD");
        t.setPressurePsi(req.pressurePsi());
        t.setInstalledAt(req.installedAt());
        t.setNextCheckDate(req.nextCheckDate());
        t.setNotes(req.notes());
        t.setCreatedBy(user);
        return tyreRepo.save(t);
    }

    @Transactional
    public void deleteTyre(UUID id) { tyreRepo.deleteById(id); }

    public record TyreRequest(
            UUID deviceId, String vehiclePlate, String position, String brand,
            String serialNo, String status, java.math.BigDecimal pressurePsi,
            java.time.LocalDate installedAt, java.time.LocalDate nextCheckDate, String notes) {}
}
