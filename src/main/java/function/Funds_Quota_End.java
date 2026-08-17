package function;

import Repo.FundsQuota;
import Repo.FundsQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service backing the "Start of Duty" dialog (Controller.Funds_Quota on the
 * frontend). Saves/updates the starting cash fund + daily quota for a
 * branch on a given date, and lets Sales.java read it back.
 *
 * Mirrors the Register_Branch_Account pattern: the controller stays thin
 * and this class owns the actual persistence + validation logic.
 */
@Service
public class Funds_Quota_End {

    private final FundsQuotaRepository fundsQuotaRepository;

    public Funds_Quota_End(FundsQuotaRepository fundsQuotaRepository) {
        this.fundsQuotaRepository = fundsQuotaRepository;
    }

    /**
     * Saves (or updates, if one already exists for this branch+date) the
     * starting fund / daily quota.
     */
    @Transactional
    public FundsQuota saveOrUpdate(Long branchId, String accountId, String dutyDateStr,
                                    double startingFund, double dailyQuota) {
        if (branchId == null) {
            throw new IllegalArgumentException("branchId is required.");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is required.");
        }
        if (dutyDateStr == null || dutyDateStr.isBlank()) {
            throw new IllegalArgumentException("dutyDate is required.");
        }
        if (startingFund < 0 || dailyQuota < 0) {
            throw new IllegalArgumentException("Values can't be negative.");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dutyDateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("dutyDate must be in yyyy-MM-dd format.");
        }

        FundsQuota entity = fundsQuotaRepository
                .findByBranchIdAndDutyDate(branchId, date)
                .orElse(new FundsQuota());

        entity.setBranchId(branchId);
        entity.setAccountId(accountId);
        entity.setDutyDate(date);
        entity.setStartingFund(startingFund);
        entity.setDailyQuota(dailyQuota);

        return fundsQuotaRepository.save(entity);
    }

    /**
     * Fetches the saved starting fund / daily quota for this branch on this
     * date, if one exists.
     */
    public Optional<FundsQuota> getForBranchAndDate(Long branchId, String dutyDateStr) {
        LocalDate date = LocalDate.parse(dutyDateStr);
        return fundsQuotaRepository.findByBranchIdAndDutyDate(branchId, date);
    }
}