package DNSFMigration;

import org.neo4j.driver.v1.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.text.Normalizer;
import java.lang.Integer;

public class DNSFMigration
{
  public static void main(String[] args)
  {
    if(args.length != 1 || args[0].equals(""))
    {
      System.err.println("Usage: java -jar DNSFMigration <Neo4j host or ip>");
      System.exit(-1);
    }

    Driver driver = GraphDatabase.driver("http://" + args[0] + "/db/data/:7474");

    try (Session session = driver.session())
    {
      StatementResult result = session.run("MATCH (u:User) RETURN u.id AS id, u.displayName AS dn;");

      List<Record> records = result.list();
      int rsize = records.size();

      Transaction tx = session.beginTransaction();
      Map<String, Object> params = new HashMap<String, Object>();
      for(int i = rsize; i-- > 0;)
      {
        Map<String, Object> user = records.get(i).asMap();

        Object id = user.get("id");
        Object dn = user.get("dn");

        if(id != null && dn != null)
        {
          params.put("uid", id);
          params.put("dnsf", DNSFMigration.generateDisplayNamePermutations(dn.toString()));

          tx.run("MATCH (u:User {id: {uid} }) SET u.displayNameSearchField = {dnsf}", params);
        }

        if(i % 1000 == 0) // Neo4j cannot buffer transactions correctly and may get OOM errors, so split the transaction in chunks...
        {
          tx.success();
          tx.close();
          tx = session.beginTransaction();
        }
      }
    }

    driver.close();
  }

	private static String generateDisplayNamePermutations(String displayName)
	{
		List<String> parts = DNSFMigration.split(removeAccents(displayName).toLowerCase().replaceAll("\\s+", " "), " ");
		StringBuilder permutations = new StringBuilder();

		if(parts.size() > 5) // Only compute the permutations for the first five terms, otherwise the string is too big
		{
			permutations.append(DNSFMigration.join(parts, ""));
			parts = parts.subList(0, 5);
		}
		permutations.append(DNSFMigration.createAllPermutations(parts));

		return permutations.toString();
  }

	private static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

  /**
   * Cutting a character string following a delimiter.
   *
   * @param regexDelim the regex delim
   * @param str the str
   *
   * @return the list<string>
   */
  private static List<String> split(String str, String regexDelim) {
      return Arrays.asList(str.split(regexDelim));
  }

  /**
   * Returns the concatenation of strings from a list using the
   * delimiter indicated.
   * Example: <code>join ([ "A", "B", "CD"], "/") -> "A / B / CD"</code>
   *
   * @param list the list of strings to concatenate.
   * @param delim the delimiter to be inserted between each value.
   *
   * @return the string containing the concatenated values.
   */
  private static String join(List<String> list, String delim) {
      boolean isDelimiter = false;
      final StringBuilder builder = new StringBuilder();
      if (list != null) {
          for (String val : list) {
              if (isDelimiter) {
                  builder.append(delim);
              }
              builder.append(val);
              isDelimiter = true;
          }
      }
      return builder.toString();
  }

    /**
     * Generates a string containing all permutations of the given string list (maximum 16 strings)
     * Example: <code>createAllPermutations ([ "A", "B", "C"]) -> "ABCACBACAB"</code>
     *
     * @param list the list of strings to permute.
     *
     * @return the string containing all permutations.
     */
    private static String createAllPermutations(List<String> list)
    {
        int size = list.size();
        if(size > 16) // The limit is 16 because 16*4 == 64 == number of bits in a long. 20! > MAX_LONG so that would be the hard limit anyway
            throw new IllegalArgumentException("The number of string to permute exceeds 16");
        else if(size == 0)
            return "";
        else if(size == 1)
            return list.get(0);

        int nbPermuts = size; // the number of permutations is size!
        for(int i = size; i-- > 1;)
            nbPermuts *= i;

        StringBuilder str = new StringBuilder();
        List<Long> permuts = new ArrayList<Long>(nbPermuts);
        Map<Long, List<Integer>> prefixIndex = new HashMap<Long, List<Integer>>();
        Set<Integer> usedPermuts = new HashSet<Integer>();

        // Recursively generate all permutations
        DNSFMigration.generateAllPermutations(permuts, prefixIndex, size, 0, 0);

        Long current = permuts.get(0);
        usedPermuts.add(new Integer(0));

        int elementsToRead = size;
        for(int i = nbPermuts; i-- > 0;)
        {
            long ptr = 0xF << ((elementsToRead - 1) * 4);
            long val = current.longValue();

            // Only append the necessary strings
            for(int e = elementsToRead; e-- > 0;)
            {
                int strToAppend = (int)((val & ptr) >>> (e * 4));
                ptr >>>= 4;
                str.append(list.get(strToAppend));
            }
            elementsToRead = 0;

            Long next = current;
            do
            {
                ++elementsToRead;
                // Find the best next permutation (aka current suffix matches the biggest next prefix possible) (e.g. aBCD should match BCDa)
                long prefixToClear = ((long)(size - elementsToRead));
                long prefixLengthToLookFor = prefixToClear << (15 * 4);
                val &= (~((long)0) ^ (0xF << (prefixToClear * 4)));

                List<Integer> nextCandidates = prefixIndex.get(new Long(prefixLengthToLookFor | val));

                if(nextCandidates == null)
                {
                    System.err.println("\n\nImpossible error in createAllPermutations: No candidates\n\n");
                    continue;
                }
                else
                {
                    // Find a permutation that hasn't already been used
                    for(int candidateIx = nextCandidates.size(); candidateIx-- > 0;)
                    {
                        Integer candidate = nextCandidates.get(candidateIx);
                        if(usedPermuts.contains(candidate) == false)
                        {
                            usedPermuts.add(candidate);
                            next = permuts.get(candidate.intValue());
                            break;
                        }
                    }
                }
            }
            // If we can't find a n-sized prefix, try a (n-1)-sized prefix on the next iteration
            while(next == current && val != 0 && usedPermuts.size() != nbPermuts);

            current = next;
        }

        if(usedPermuts.size() != nbPermuts)
          System.err.println("\n\nImpossible error in createAllPermutations: Used " + usedPermuts.size() + " out of " + nbPermuts + "\n\n");

        return str.toString();
    }

    // nbElements must be at most 16. Call with permut = 0, taken = 0
    private static void generateAllPermutations(List<Long> allPermuts, Map<Long, List<Integer>> prefixIndex, int nbElements,
        long permut, long taken)
    {
        boolean isFinished = true;
        for(int i = nbElements; i-- > 0;)
        {
            if((taken & (1 << i)) != 0)
                continue;
            else
            {
                isFinished = false;
                generateAllPermutations(allPermuts, prefixIndex, nbElements, (permut << 4) | i, taken | (1 << i));
            }
        }

        if(isFinished == true)
        {
            Long finalPermut = new Long(permut);
            Integer permutIx = new Integer(allPermuts.size());
            allPermuts.add(finalPermut);

            for(long i = nbElements; i-- > 1;)
            {
                permut >>>= 4;

                // The prefixed length makes it possible to distinguish 0x01 and 0x1
                long prefixLength = (i << (15 * 4));
                Long prefix = new Long(prefixLength | permut);

                List<Integer> prefixes = prefixIndex.get(prefix);
                if(prefixes == null)
                {
                    prefixes = new ArrayList<Integer>(nbElements); // Real capacity should be (nbElements - 1)! but this is good enough
                    prefixIndex.put(prefix, prefixes);
                }
                prefixes.add(permutIx);
            }
        }
    }
}
