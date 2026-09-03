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
 * SPECIFIC ACCOUNT on a branch, on a given date, and lets Sales.java read it
 * back scoped to that same account.
 *
 * FIX: previously lookups were keyed only by (branchId, dutyDate), so every
 * cashier logged into the same branch on the same day shared ONE row —
 * whoever set the starting fund/quota last overwrote it for everyone else.
 * Now every lookup/save is keyed by (branchId, accountId, dutyDate), so each
 * cashier has their own isolated row.
 */
@Service
public class FundsQuotaService {

    private final FundsQuotaRepository fundsQuotaRepository;

    public FundsQuotaService(FundsQuotaRepository fundsQuotaRepository) {
        this.fundsQuotaRepository = fundsQuotaRepository;
    }

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

        // FIX: lookup is now scoped to this specific account too, not just
        // branch+date — so saving here never overwrites another cashier's row.
        FundsQuota entity = fundsQuotaRepository
                .findByBranchIdAndAccountIdAndDutyDate(branchId, accountId, date)
                .orElse(new FundsQuota());

        entity.setBranchId(branchId);
        entity.setAccountId(accountId);
        entity.setDutyDate(date);
        entity.setStartingFund(startingFund);
        entity.setDailyQuota(dailyQuota);
        return fundsQuotaRepository.save(entity);
    }

    /**
     * FIX: now requires accountId too, so a cashier only ever reads back
     * THEIR OWN starting fund/quota for this branch/date, never another
     * cashier's.
     */
    public Optional<FundsQuota> getForBranchAndAccountAndDate(Long branchId, String accountId, String dutyDateStr) {
        LocalDate date = LocalDate.parse(dutyDateStr);
        return fundsQuotaRepository.findByBranchIdAndAccountIdAndDutyDate(branchId, accountId, date);
    }
}