CREATE INDEX users_role_name_idx ON users(role, name);
CREATE INDEX medications_patient_name_idx ON medications(patient_id, name);
CREATE INDEX health_records_patient_date_idx ON health_records(patient_id, recorded_at);
CREATE INDEX health_records_provider_date_idx ON health_records(provider_id, recorded_at);
CREATE INDEX invoices_patient_due_idx ON invoices(patient_id, due_at);
CREATE INDEX vitals_patient_date_idx ON vitals(patient_id, recorded_at);
CREATE INDEX appointments_provider_status_patient_idx ON appointments(provider_id, status, patient_id);
