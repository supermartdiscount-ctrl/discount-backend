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
 * Every lookup/save is keyed by (branchId, accountId, dutyDate), so each
 * cashier has their own isolated row and never overwrites another
 * cashier's starting fund/quota for the same branch/day.
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

    public Optional<FundsQuota> getForBranchAndAccountAndDate(Long branchId, String accountId, String dutyDateStr) {
        LocalDate date = LocalDate.parse(dutyDateStr);
        return fundsQuotaRepository.findByBranchIdAndAccountIdAndDutyDate(branchId, accountId, date);
    }
}